package io.github.benxincaomu.oa.base.init;

import java.time.Duration;
import java.util.Comparator;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.init.vo.UserInitVo;
import io.github.benxincaomu.oa.base.security.KeycloakAdminClient;
import io.github.benxincaomu.oa.base.security.KeycloakBootstrapIdentity;
import io.github.benxincaomu.oa.base.security.KeycloakObjectLink;
import io.github.benxincaomu.oa.base.security.KeycloakObjectLinkRepository;
import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentRepository;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUser;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.tenant.Tenant;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.RoleRepository;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

/**
 * First-run and existing-installation initialization.
 *
 * <p>
 * Keycloak owns users, groups, roles, and permissions. OA local rows created or
 * reused here are only the stable numeric-ID projections and historical snapshots
 * that workflow and audit data reference; no local permission, role-grant, or
 * user-role authorization data is seeded.
 * </p>
 *
 * <p>
 * Initialization state is derived from the database (a tenant row exists), never
 * from the Redis cache, so a database reset cannot leave the application thinking
 * it is still initialized while Redis holds an old value.
 * </p>
 */
@Service
public class InitService {
    private static final String INITIALIZING_KEY = "initializeing";
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 10;
    private static final int EMAIL_MAX_LENGTH = 50;
    private static final int MOBILE_MAX_LENGTH = 16;

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private RedisTemplate<String, Boolean> redisTemplate;

    @Resource
    private TenantRepository tenantRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private RoleRepository roleRepository;

    @Resource
    private DepartmentUserRepository departmentUserRepository;

    @Resource
    private KeycloakObjectLinkRepository keycloakObjectLinkRepository;

    @Resource
    private KeycloakAdminClient keycloakAdminClient;

    @Resource
    private OaBaselineAuthorizationProvisioner baselineAuthorizationProvisioner;

    /**
     * 项目是否已有租户数据（以数据库为准，不读 Redis 缓存）。
     */
    public boolean isInitialized() {
        return tenantRepository.count() > 0;
    }

    /**
     * 管理员、根部门、管理员角色是否都已建立 Keycloak 链接。
     * 有租户数据但链接缺失时，仍需要走一次“存量安装”初始化来补齐 Keycloak 侧信息。
     */
    public boolean hasKeycloakLinks() {
        return keycloakObjectLinkRepository.existsByObjectType(KeycloakObjectLink.USER)
                && keycloakObjectLinkRepository.existsByObjectType(KeycloakObjectLink.DEPARTMENT)
                && keycloakObjectLinkRepository.existsByObjectType(KeycloakObjectLink.ROLE);
    }

    /**
     * 全新安装：创建租户、管理员数字 ID 快照、根部门，并把 Keycloak 对象链接到这些数字 ID，
     * 最后写入租户标记与基线授权资源。
     */
    @Transactional
    public void init(Department department, User user, Role role, KeycloakBootstrapIdentity keycloakIdentity) {
        if (isInitialized()) {
            return;
        }
        acquireInitializationLock();
        try {
            if (isInitialized()) {
                return;
            }

            Tenant tenant = initTenant();
            user.setTenantId(tenant.getId());
            user = initRootUser(user);
            role.setTenantId(tenant.getId());
            role = initRootRole(role);
            department.setTenantId(tenant.getId());
            department.setLeaderUserId(user.getId());
            department = initRootDepartment(department);

            DepartmentUser departmentUser = new DepartmentUser();
            departmentUser.setUserId(user.getId());
            departmentUser.setDepartmentId(department.getId());
            departmentUser.setTenantId(tenant.getId());
            departmentUserRepository.save(departmentUser);

            saveKeycloakLinkIfAbsent(KeycloakObjectLink.USER, keycloakIdentity.userId(), user.getId());
            saveKeycloakLinkIfAbsent(KeycloakObjectLink.DEPARTMENT, keycloakIdentity.groupId(), department.getId());
            saveKeycloakLinkIfAbsent(KeycloakObjectLink.ROLE, keycloakIdentity.roleId(), role.getId());

            markTenantRootGroup(keycloakIdentity.groupId(), tenant.getId(), user.getId());
            baselineAuthorizationProvisioner.provision(tenant.getId());
        } finally {
            releaseInitializationLock();
        }
    }

    /**
     * 存量安装：数据库里已经有租户/部门/工作流数据，但没有 Keycloak 配置。
     * 只补充 Keycloak 侧配置与链接，复用现有数字 ID，不创建新的租户或部门。
     */
    @Transactional
    public void completeExistingInstallation(UserInitVo administrator, KeycloakBootstrapIdentity keycloakIdentity) {
        if (!isInitialized()) {
            throw new IllegalStateException("数据库中没有租户数据，请使用首次初始化流程");
        }
        acquireInitializationLock();
        try {
            Tenant tenant = tenantRepository.findAll().stream()
                    .min(Comparator.comparing(Tenant::getId))
                    .orElseThrow(() -> new IllegalStateException("未找到租户记录，无法完成存量安装初始化"));
            Long tenantId = tenant.getId();
            Department rootDepartment = findRootDepartment(tenantId)
                    .orElseThrow(() -> new IllegalStateException("未找到租户根部门，无法绑定 Keycloak 根组"));
            User administratorUser = resolveAdministratorUser(administrator, tenantId);
            Role administratorRole = resolveAdministratorRole(tenantId);

            saveKeycloakLinkIfAbsent(KeycloakObjectLink.USER, keycloakIdentity.userId(), administratorUser.getId());
            saveKeycloakLinkIfAbsent(KeycloakObjectLink.DEPARTMENT, keycloakIdentity.groupId(),
                    rootDepartment.getId());
            saveKeycloakLinkIfAbsent(KeycloakObjectLink.ROLE, keycloakIdentity.roleId(), administratorRole.getId());

            ensureDepartmentMembership(rootDepartment, administratorUser, tenantId);
            markTenantRootGroup(keycloakIdentity.groupId(), tenantId, administratorUser.getId());
            baselineAuthorizationProvisioner.provision(tenantId);
        } finally {
            releaseInitializationLock();
        }
    }

    /**
     * 存量安装初始化时，用来把 Keycloak 根组绑定到现有的本地根部门。
     */
    public Optional<String> findRootDepartmentName() {
        return tenantRepository.findAll().stream()
                .min(Comparator.comparing(Tenant::getId))
                .flatMap(tenant -> findRootDepartment(tenant.getId()).map(Department::getName));
    }

    private void markTenantRootGroup(String groupId, Long tenantId, Long administratorUserId) {
        keycloakAdminClient.setGroupAttribute(groupId, "tenant_id", String.valueOf(tenantId));
        keycloakAdminClient.setGroupAttribute(groupId, "leader_user_id", String.valueOf(administratorUserId));
    }

    private Tenant initTenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantName("default");
        tenant.setEnable(true);
        return tenantRepository.save(tenant);
    }

    private Role initRootRole(Role role) {
        return roleRepository.save(role);
    }

    private Department initRootDepartment(Department department) {
        return departmentRepository.save(department);
    }

    private User initRootUser(User user) {
        user.setPassword(null);
        user.setSalt(null);
        user.setEnable(true);
        user.setRawPassword(false);
        return userRepository.save(user);
    }

    private Optional<Department> findRootDepartment(Long tenantId) {
        return departmentRepository.findByTenantId(tenantId).stream()
                .filter(department -> department.getParentId() == null)
                .min(Comparator.comparing(Department::getId));
    }

    /**
     * 存量安装初始化时选择要绑定到 Keycloak 管理员的本地用户：
     * 优先同名用户，其次租户内 id 最小的用户，最后按提交的信息新建一条数字 ID 快照。
     */
    private User resolveAdministratorUser(UserInitVo administrator, Long tenantId) {
        User user = userRepository.findByUserNameAndTenantId(administrator.getUserName(), tenantId);
        if (user == null) {
            user = userRepository.findByTenantId(tenantId).stream()
                    .min(Comparator.comparing(User::getId))
                    .orElse(null);
        }
        if (user == null) {
            user = new User();
            user.setTenantId(tenantId);
            user.setUserName(limit(administrator.getUserName(), USERNAME_MAX_LENGTH));
            user.setName(limit(administrator.getName(), NAME_MAX_LENGTH));
            user.setEmail(limit(administrator.getEmail(), EMAIL_MAX_LENGTH));
            user.setMobile(limit(administrator.getMobile(), MOBILE_MAX_LENGTH));
        }
        user.setEnable(true);
        user.setPassword(null);
        user.setSalt(null);
        user.setRawPassword(false);
        return userRepository.save(user);
    }

    /**
     * 存量安装初始化时选择要绑定 Keycloak 管理员角色的本地角色：
     * 优先名为 oa-admin 的角色，其次租户内 id 最小的角色，最后新建一条快照。
     */
    private Role resolveAdministratorRole(Long tenantId) {
        Role role = roleRepository.findByNameAndTenantId(KeycloakAdminClient.OA_ADMIN_CLIENT_ROLE, tenantId);
        if (role == null) {
            role = roleRepository.findByTenantId(tenantId).stream()
                    .min(Comparator.comparing(Role::getId))
                    .orElse(null);
        }
        if (role == null) {
            role = new Role();
            role.setTenantId(tenantId);
            role.setName(KeycloakAdminClient.OA_ADMIN_CLIENT_ROLE);
        }
        return roleRepository.save(role);
    }

    private void ensureDepartmentMembership(Department department, User user, Long tenantId) {
        boolean exists = departmentUserRepository.findAllByDepartmentId(department.getId()).stream()
                .anyMatch(membership -> user.getId().equals(membership.getUserId()));
        if (exists) {
            return;
        }
        DepartmentUser membership = new DepartmentUser();
        membership.setUserId(user.getId());
        membership.setDepartmentId(department.getId());
        membership.setTenantId(tenantId);
        departmentUserRepository.save(membership);
    }

    private void saveKeycloakLinkIfAbsent(String objectType, String externalId, Long localId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalStateException("Keycloak 初始化未返回有效的" + objectType + " ID");
        }
        Optional<KeycloakObjectLink> byExternal = keycloakObjectLinkRepository
                .findByObjectTypeAndExternalId(objectType, externalId);
        if (byExternal.isPresent()) {
            if (!byExternal.get().getLocalId().equals(localId)) {
                throw new IllegalStateException("Keycloak 对象已链接到另一个 OA 数字 ID: " + objectType);
            }
            return;
        }
        keycloakObjectLinkRepository.findByObjectTypeAndLocalId(objectType, localId).ifPresent(existing -> {
            if (!existing.getExternalId().equals(externalId)) {
                throw new IllegalStateException("OA 对象已链接到另一个 Keycloak 对象: " + objectType);
            }
        });
        KeycloakObjectLink link = new KeycloakObjectLink();
        link.setObjectType(objectType);
        link.setExternalId(externalId);
        link.setLocalId(localId);
        keycloakObjectLinkRepository.save(link);
    }

    private void acquireInitializationLock() {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(INITIALIZING_KEY, true, Duration.ofSeconds(30));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalStateException("项目正在初始化，请稍后重试");
        }
    }

    private void releaseInitializationLock() {
        redisTemplate.delete(INITIALIZING_KEY);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

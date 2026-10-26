package io.github.benxincaomu.oa.base.init;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

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
 * First-run initialization.
 *
 * <p>
 * Keycloak owns users, groups, roles, and permissions. OA local rows created
 * here are only the stable numeric-ID projections and historical snapshots that
 * workflow and audit data reference; no local permission, role-grant, or
 * user-role authorization data is seeded any more.
 * </p>
 */
@Service
public class InitService {

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

    private final String isInitializedKey = "projectIsInitialized";

    /**
     * 判断项目是否已经初始化
     *
     * @return
     */
    public boolean isInitialized() {
        Boolean initialized = redisTemplate.opsForValue().get(isInitializedKey);
        if (initialized == null) {
            if (tenantRepository.count() == 0) {
                redisTemplate.opsForValue().set(isInitializedKey, false);
                initialized = false;
            } else {
                redisTemplate.opsForValue().set(isInitializedKey, true);
                initialized = true;
            }
        }
        return initialized;
    }

    @Transactional
    public void init(Department department, User user, Role role, KeycloakBootstrapIdentity keycloakIdentity) {
        if (isInitialized()) {
            return;
        }
        String initializingKey = "initializeing";
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(initializingKey, true, Duration.ofSeconds(30));
        if (!Boolean.TRUE.equals(acquired)) {
            if (isInitialized()) {
                return;
            }
            throw new IllegalStateException("项目正在初始化，请稍后重试");
        }
        try {
            if (isInitialized()) {
                return;
            }

            Tenant tenant = initTenant();
            // 本地只保留数字 ID 投影与历史快照，不再创建本地权限/授权种子
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

            saveKeycloakLink(KeycloakObjectLink.USER, keycloakIdentity.userId(), user.getId());
            saveKeycloakLink(KeycloakObjectLink.DEPARTMENT, keycloakIdentity.groupId(), department.getId());
            saveKeycloakLink(KeycloakObjectLink.ROLE, keycloakIdentity.roleId(), role.getId());

            // Keycloak 侧：租户标记 + 基线菜单/接口资源 + oa-admin 默认授权
            keycloakAdminClient.setGroupAttribute(keycloakIdentity.groupId(), "tenant_id",
                    String.valueOf(tenant.getId()));
            baselineAuthorizationProvisioner.provision(tenant.getId());
            // 这里不写 isInitializedKey：事务提交后由 InitController 调用 markInitialized，
            // 避免事务回滚后 Redis 已经标记为“已初始化”。
        } finally {
            redisTemplate.delete(initializingKey);
        }
    }

    /**
     * 初始化事务成功提交后再标记为已完成，供 InitController 在 init() 返回后调用。
     */
    public void markInitialized() {
        redisTemplate.opsForValue().set(isInitializedKey, true);
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

    private void saveKeycloakLink(String objectType, String externalId, Long localId) {
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalStateException("Keycloak 初始化未返回有效的" + objectType + " ID");
        }
        KeycloakObjectLink link = new KeycloakObjectLink();
        link.setObjectType(objectType);
        link.setExternalId(externalId);
        link.setLocalId(localId);
        keycloakObjectLinkRepository.save(link);
    }

}

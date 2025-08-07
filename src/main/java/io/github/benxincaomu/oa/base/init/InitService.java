package io.github.benxincaomu.oa.base.init;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentRepository;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUser;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.tenant.Tenant;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;
import io.github.benxincaomu.oa.bussiness.user.Permission;
import io.github.benxincaomu.oa.bussiness.user.PermissionRepository;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.RolePermission;
import io.github.benxincaomu.oa.bussiness.user.RolePermissionRepository;
import io.github.benxincaomu.oa.bussiness.user.RoleRepository;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
import io.github.benxincaomu.oa.bussiness.user.UserRole;
import io.github.benxincaomu.oa.bussiness.user.UserRoleRepository;
import io.github.benxincaomu.oa.bussiness.user.UserService;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Service
public class InitService {

    @Resource
    private DepartmentRepository departmentRepository;

    @Resource
    private RedisTemplate<String, Boolean> redisTemplate;

    @Resource
    private TenantRepository tenantRepository;

    @Resource
    private PermissionRepository permissionRepository;

    @Resource
    private UserRepository userRepository;

    @Resource
    private RoleRepository roleRepository;
    @Resource
    private UserRoleRepository userRoleRepository;

    @Resource
    private RolePermissionRepository rolePermissionRepository;
    @Resource
    private DepartmentUserRepository departmentUserRepository;

    @Resource
    private UserService userService;

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
    public void init(Department department, User user, Role role) {
        if (!isInitialized()) {
            String initializeingKey = "initializeing";
            if (redisTemplate.opsForValue().setIfAbsent(initializeingKey, true, Duration.ofSeconds(30)) == true && !isInitialized()) {

                Tenant tenant = initTenant();
                // 权限
                List<Permission> permissions = initPermissions(tenant.getId());
                // 用户
                user.setTenantId(tenant.getId());
                initRootUser(user);
                // 角色
                role.setTenantId(tenant.getId());
                initRootRole(role);
                // 用户角色关联
                UserRole userRole = new UserRole();
                userRole.setUserId(user.getId());
                userRole.setRoleId(role.getId());
                userRole.setTenantId(tenant.getId());
                userRoleRepository.save(userRole);
                // 分配权限
                assignPermissionToRole(role, permissions);
                ;
                //
                department.setTenantId(tenant.getId());
                department.setLeaderUserId(user.getId());
                department = initRootDepartment(department);

                DepartmentUser departmentUser = new DepartmentUser();
                departmentUser.setUserId(user.getId());
                departmentUser.setDepartmentId(department.getId());
                departmentUser.setTenantId(tenant.getId());
                tenant.setTenantId(tenant.getId());
                departmentUserRepository.save(departmentUser);
                redisTemplate.delete(initializeingKey);
                redisTemplate.opsForValue().set(isInitializedKey, true);
            }

        }
    }

    private Tenant initTenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantName("default");
        tenant.setEnable(true);
        return tenantRepository.save(tenant);
    }

    private List<Permission> initPermissions(Long tenantId) {
        List<Permission> permissions = new ArrayList<>();
        // 系统管理
        Permission systemManagement = new Permission();
        systemManagement.setName("系统管理");
        systemManagement.setType(Permission.TYPE_1);
        systemManagement.setTenantId(tenantId);
        permissionRepository.save(systemManagement);
        permissions.add(systemManagement);
        // 权限管理
        Permission permissionManagement = new Permission();
        permissionManagement.setName("权限管理");
        permissionManagement.setType(Permission.TYPE_2);
        permissionManagement.setParentId(systemManagement.getId());
        permissionManagement.setParentName(systemManagement.getName());
        permissionManagement.setTenantId(tenantId);
        permissionManagement.setValue("/app/permissions");
        permissionRepository.save(permissionManagement);
        permissions.add(permissionManagement);
        // 权限相关url
        Permission permissionUrl = new Permission();
        permissionUrl.setName("权限相关url");
        permissionUrl.setType(Permission.TYPE_3);
        permissionUrl.setValue("/permission/**");
        permissionUrl.setTenantId(tenantId);
        permissionUrl.setParentId(permissionManagement.getId());
        permissionUrl.setParentName(permissionManagement.getName());
        permissionRepository.save(permissionUrl);
        permissions.add(permissionUrl);
        // 用户管理
        Permission userManagement = new Permission();
        userManagement.setName("用户管理");
        userManagement.setType(Permission.TYPE_2);
        userManagement.setValue("/app/usermanage");
        userManagement.setParentId(systemManagement.getId());
        permissionUrl.setParentName(permissionManagement.getName());
        userManagement.setTenantId(tenantId);
        permissionRepository.save(userManagement);
        permissions.add(userManagement);
        // 用户相关url
        Permission userUrlPermission = new Permission();
        userUrlPermission.setName("用户相关url");
        userUrlPermission.setType(Permission.TYPE_3);
        userUrlPermission.setValue("/user/**");
        userUrlPermission.setParentId(userManagement.getId());
        userUrlPermission.setParentName(userManagement.getName());
        userUrlPermission.setTenantId(tenantId);
        permissionRepository.save(userUrlPermission);
        permissions.add(userUrlPermission);
        // 个人信息相关url权限
        Permission personalInfo = new Permission();
        personalInfo.setName("个人信息相关url");
        personalInfo.setType(Permission.TYPE_3);
        personalInfo.setValue("/login/**");
        permissionRepository.save(personalInfo);
        permissions.add(personalInfo);
        // 角色管理
        Permission roleManagement = new Permission();
        roleManagement.setName("角色管理");
        roleManagement.setType(Permission.TYPE_2);
        roleManagement.setValue("/app/roles");
        roleManagement.setParentId(systemManagement.getId());
        roleManagement.setParentName(systemManagement.getName());
        roleManagement.setTenantId(tenantId);
        permissionRepository.save(roleManagement);
        permissions.add(roleManagement);

        // 角色相关url
        Permission roleUrlPermission = new Permission();
        roleUrlPermission.setName("角色相关url");
        roleUrlPermission.setType(Permission.TYPE_3);
        roleUrlPermission.setValue("/role/**");
        roleUrlPermission.setParentId(roleManagement.getId());
        roleUrlPermission.setParentName(roleManagement.getName());
        roleUrlPermission.setTenantId(tenantId);
        permissionRepository.save(roleUrlPermission);
        permissions.add(roleUrlPermission);
        // 组织管理
        Permission organizationManagement = new Permission();
        organizationManagement.setName("组织管理");
        organizationManagement.setType(Permission.TYPE_2);
        organizationManagement.setValue("/app/organizes");
        organizationManagement.setParentId(systemManagement.getId());
        organizationManagement.setParentName(systemManagement.getName());
        organizationManagement.setTenantId(tenantId);
        permissionRepository.save(organizationManagement);
        permissions.add(organizationManagement);

        // 组织相关url
        Permission orgUrlPermission = new Permission();
        orgUrlPermission.setName("组织相关url");
        orgUrlPermission.setType(Permission.TYPE_3);
        orgUrlPermission.setValue("/organize/**");
        orgUrlPermission.setParentId(organizationManagement.getId());
        orgUrlPermission.setParentName(organizationManagement.getName());
        orgUrlPermission.setTenantId(tenantId);
        permissionRepository.save(orgUrlPermission);
        permissions.add(orgUrlPermission);

        // 设计工作台
        Permission designWorkbench = new Permission();
        designWorkbench.setName("设计工作台");
        designWorkbench.setType(Permission.TYPE_2);
        designWorkbench.setValue("/app/organizes");
        designWorkbench.setTenantId(tenantId);
        permissionRepository.save(designWorkbench);
        permissions.add(designWorkbench);
        // 设计工作台相关url
        Permission designWorkbenchUrlPermission = new Permission();
        designWorkbenchUrlPermission.setName("设计工作台相关url");
        designWorkbenchUrlPermission.setType(Permission.TYPE_3);
        designWorkbenchUrlPermission.setValue("/workbench/**");
        designWorkbenchUrlPermission.setParentId(designWorkbench.getId());
        designWorkbenchUrlPermission.setParentName(designWorkbench.getName());
        permissionRepository.save(designWorkbenchUrlPermission);
        permissions.add(designWorkbenchUrlPermission);
        // 设计工作台实体相关url
        Permission entityUrlPermission = new Permission();
        entityUrlPermission.setName("实体设计相关url");
        entityUrlPermission.setType(Permission.TYPE_3);
        entityUrlPermission.setValue("/entityDefinition/**");
        entityUrlPermission.setParentId(designWorkbench.getId());
        entityUrlPermission.setParentName(designWorkbench.getName());
        entityUrlPermission.setTenantId(tenantId);
        permissionRepository.save(entityUrlPermission);
        permissions.add(entityUrlPermission);

        // 流程定义相关url
        Permission flowUrlPermission = new Permission();
        flowUrlPermission.setName("流程定义相关url");
        flowUrlPermission.setType(Permission.TYPE_3);
        flowUrlPermission.setValue("/workflowDefinition/**");
        flowUrlPermission.setParentId(designWorkbench.getId());
        flowUrlPermission.setParentName(designWorkbench.getName());
        flowUrlPermission.setTenantId(tenantId);
        permissionRepository.save(flowUrlPermission);
        permissions.add(flowUrlPermission);

        // 文件上传权限
        Permission uploadPermission = new Permission();
        uploadPermission.setName("文件上传权限相关url");
        uploadPermission.setType(Permission.TYPE_3);
        uploadPermission.setValue("/file/**");
        uploadPermission.setTenantId(tenantId);
        permissions.add(uploadPermission);

        return permissions;
    }

    private Role initRootRole(Role role) {

        return roleRepository.save(role);
    }

    private Department initRootDepartment(Department department) {

        return departmentRepository.save(department);
    }

    private User initRootUser(User user) {

        return userService.insert(user);
    }

    private void assignPermissionToRole(Role role, List<Permission> permissions) {
        for (Permission permission : permissions) {
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(role.getId());
            rolePermission.setPermissionId(permission.getId());
            rolePermission.setTenantId(role.getTenantId());
            rolePermissionRepository.save(rolePermission);
        }
    }

}

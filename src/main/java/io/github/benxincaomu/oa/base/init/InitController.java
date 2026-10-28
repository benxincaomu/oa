package io.github.benxincaomu.oa.base.init;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.init.vo.InitVo;
import io.github.benxincaomu.oa.base.security.KeycloakAdminClient;
import io.github.benxincaomu.oa.base.security.KeycloakBootstrapIdentity;
import io.github.benxincaomu.oa.base.security.KeycloakConnectionService;
import io.github.benxincaomu.oa.base.security.KeycloakConnectionStatusVo;
import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.User;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;

/**
 * 初始化相关。
 *
 * <p>
 * 支持两种模式：
 * <ul>
 * <li>全新安装：数据库没有租户，创建租户/根部门/管理员数字 ID 并配置 Keycloak；</li>
 * <li>存量安装：数据库已有租户但 {@code keycloak_integration_config} 为空，
 * 只补充 Keycloak 配置并复用现有租户、根部门和管理员数字 ID。</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("initProject")
public class InitController {

    @Resource
    private InitService initService;

    @Resource
    private KeycloakConnectionService keycloakConnectionService;

    @Resource
    private KeycloakAdminClient keycloakAdminClient;

    @PostMapping()
    public void init(@RequestBody @Valid InitVo initVo) {
        boolean projectInitialized = initService.isInitialized();
        boolean keycloakConfigured = keycloakConnectionService.getConfiguration().isPresent();
        if (projectInitialized && keycloakConfigured) {
            throw new IllegalStateException("项目已初始化，不能重复初始化");
        }
        if (!projectInitialized && initVo.getDepartment() == null) {
            throw new IllegalArgumentException("首次初始化必须填写顶级部门");
        }

        keycloakConnectionService.configure(initVo.getKeycloak());

        if (projectInitialized) {
            // 存量安装：只配置 Keycloak，复用已有租户/根部门/管理员数字 ID
            String rootGroupName = initService.findRootDepartmentName()
                    .orElseThrow(() -> new IllegalStateException("未找到租户根部门，无法完成 Keycloak 初始化"));
            KeycloakBootstrapIdentity keycloakIdentity = keycloakAdminClient.bootstrapAdministrator(
                    initVo.getUser(), rootGroupName);
            initService.completeExistingInstallation(initVo.getUser(), keycloakIdentity);
            return;
        }

        Department department = new Department();
        department.setName(initVo.getDepartment().getName());
        department.setCode(initVo.getDepartment().getCode());
        User user = new User();
        user.setUserName(initVo.getUser().getUserName());
        user.setName(initVo.getUser().getName());
        user.setEmail(initVo.getUser().getEmail());
        user.setMobile(initVo.getUser().getMobile());
        Role role = new Role();
        role.setName(KeycloakAdminClient.OA_ADMIN_CLIENT_ROLE);

        KeycloakBootstrapIdentity keycloakIdentity = keycloakAdminClient.bootstrapAdministrator(initVo.getUser(),
                department.getName());
        initService.init(department, user, role, keycloakIdentity);
    }


    @GetMapping("isInitialized")
    public boolean isInitialized() {
        return initService.isInitialized();
    }

    @GetMapping("keycloakStatus")
    public KeycloakConnectionStatusVo keycloakStatus() {
        return keycloakConnectionService.getStatus();
    }

}

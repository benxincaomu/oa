package io.github.benxincaomu.oa.base.init;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.init.vo.InitStatusVo;
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
 * Keycloak 连接配置来自 {@code app.keycloak.*}（application.yml / 环境变量），
 * 不再由初始化页面填写或写入数据库。初始化本身只负责：
 * </p>
 * <ul>
 * <li>全新安装：创建租户、根部门、管理员数字 ID 快照并补齐 Keycloak 链接；</li>
 * <li>存量安装：数据库已有租户但 Keycloak 链接缺失时，复用现有数字 ID，补齐链接、
 * 租户标记和基线授权资源。</li>
 * </ul>
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
        boolean keycloakLinked = initService.hasKeycloakLinks();
        if (keycloakConnectionService.getConfiguration().isEmpty()) {
            throw new IllegalStateException(
                    "Keycloak 尚未配置，请先在 application.yml 或环境变量中设置 app.keycloak.* 并重启应用");
        }
        if (projectInitialized && keycloakLinked) {
            throw new IllegalStateException("项目已初始化，不能重复初始化");
        }
        if (!projectInitialized && initVo.getDepartment() == null) {
            throw new IllegalArgumentException("首次初始化必须填写顶级部门");
        }

        // 先校验 OIDC Discovery、回调地址与管理客户端权限，再写任何本地数据
        keycloakConnectionService.validateConfiguration();

        if (projectInitialized) {
            // 存量安装：配置来自配置文件，只补齐 Keycloak 链接/租户标记/基线资源
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

    @GetMapping("status")
    public InitStatusVo status() {
        return new InitStatusVo(initService.isInitialized(),
                keycloakConnectionService.getConfiguration().isPresent(),
                initService.hasKeycloakLinks());
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

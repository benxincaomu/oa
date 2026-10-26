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
 * 初始化相关
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
        if (initService.isInitialized()) {
            throw new IllegalStateException("项目已初始化，不能重复初始化");
        }

        keycloakConnectionService.configure(initVo.getKeycloak());

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
        // 本地事务已提交，再标记初始化完成，避免失败时留下“已完成”标记
        initService.markInitialized();
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

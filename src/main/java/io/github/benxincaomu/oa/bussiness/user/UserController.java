package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakDirectoryService;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;
import jakarta.annotation.Resource;

/**
 * 工作流表单使用的只读用户选择接口，数据源是 Keycloak。
 *
 * <p>
 * 用户管理已迁移到 {@code /identity} 接口；本地用户表的读写接口已下线，
 * 以免出现“两个数据源”。这里保留的两个接口只返回数字 ID + 显示名，
 * 供 BPMN 设计器和表单选择审批人使用。
 * </p>
 */
@RestController
@RequestMapping("user")
public class UserController {

    @Resource
    private KeycloakDirectoryService directoryService;

    @GetMapping("/findUsersByNameLike/{name}")
    public List<UserIdNameVo> findUsersByNameLike(@PathVariable("name") String name) {
        return directoryService.listTenantUsersForSelection(currentTenantId(), name);
    }

    @GetMapping("/getAllUsers")
    public List<UserIdNameVo> getAllUsers() {
        return directoryService.listTenantUsersForSelection(currentTenantId(), null);
    }

    private Long currentTenantId() {
        Long tenantId = JpaAuditorAware.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("An OA tenant context is required");
        }
        return tenantId;
    }
}

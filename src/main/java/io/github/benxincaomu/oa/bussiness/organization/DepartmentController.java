package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakDirectoryService;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;
import jakarta.annotation.Resource;

/**
 * 工作流表单使用的只读组织选择接口，数据源是 Keycloak 组。
 *
 * <p>
 * 组织管理已迁移到 {@code /identity} 接口。这些接口返回数字部门 ID，
 * 供 BPMN 设计器和流程表单选择候选部门使用，因此不能再限制为管理员，
 * 也不能读本地 {@code department} 表。
 * </p>
 */
@RestController
@RequestMapping("/organize")
public class DepartmentController {

    @Resource
    private KeycloakDirectoryService directoryService;

    @GetMapping("/listTree")
    public List<DeptVo> listTree() {
        return directoryService.listTenantDepartmentTree(currentTenantId());
    }

    @GetMapping("/listAll")
    public List<DeptVo> listAll() {
        return directoryService.listTenantDepartments(currentTenantId());
    }

    @GetMapping("/getUsersByDeptId/{id}")
    public List<UserIdNameVo> getUsersByDeptId(@PathVariable("id") Long deptId) {
        return directoryService.listDepartmentUsers(currentTenantId(), deptId);
    }

    private Long currentTenantId() {
        Long tenantId = JpaAuditorAware.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("An OA tenant context is required");
        }
        return tenantId;
    }
}

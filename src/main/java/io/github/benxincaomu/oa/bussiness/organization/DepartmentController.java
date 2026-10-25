package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakDirectoryService;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptLeaderVo;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;
import jakarta.websocket.server.PathParam;

@RestController
@RequestMapping("/organize")
@PreAuthorize("hasAuthority('ROLE_oa-admin')")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final KeycloakDirectoryService directoryService;

    DepartmentController(DepartmentService departmentService, KeycloakDirectoryService directoryService) {
        this.departmentService = departmentService;
        this.directoryService = directoryService;
    }

    /**
     * 部门列表
     *
     * @param name     部门名称
     * @param code     部门代码
     * @param currPage 当前页
     * @param pageSize 分页大小
     * @return
     */
    @GetMapping("list")
    public PagedModel<Department> list(String name, String code, Integer currPage, Integer pageSize) {
        return new PagedModel<>(departmentService.list(name, code, currPage, pageSize));
    }

    @PostMapping
    public void insert(@RequestBody Department department) {
        throw new IllegalStateException("组织数据由 Keycloak 管理，请使用 /identity 接口");
    }

    @GetMapping("/{id}")
    public Department getById(@PathParam("id") Long id) {
        return departmentService.getById(id);

    }

    @GetMapping("/listTree")
    public List<DeptVo> listTree() {
        return directoryService.listTenantDepartmentTree(currentTenantId());
    }

    @GetMapping("/listAll")
    public List<DeptVo> listAll() {
        return directoryService.listTenantDepartments(currentTenantId());
    }

    @PostMapping("/assignLeader")
    public void assignLeader(@RequestBody @Validated DeptLeaderVo vo) {
        throw new IllegalStateException("部门负责人由 Keycloak 组属性管理，请使用 /identity 接口");
    }

    @GetMapping("/getUsersByDeptId/{id}")
    public List<UserIdNameVo> getUsersByDeptId(@PathVariable("id") Long deptId){
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

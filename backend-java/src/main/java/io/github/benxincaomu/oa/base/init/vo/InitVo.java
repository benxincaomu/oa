package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.constraints.NotNull;

public class InitVo {
    
    /**
     * 管理员用户
     */
    @NotNull(message = "管理员用户信息不能为空")
    private UserInitVo user;

    /**
     * 管理员角色
     */
    @NotNull(message = "管理员角色信息不能为空")
    private RoleInitVo role;

    /**
     * 顶级部门名称
     */
    @NotNull(message = "顶级部门名称不能为空")
    private DepartmentInitVo department;

    public UserInitVo getUser() {
        return user;
    }

    public void setUser(UserInitVo user) {
        this.user = user;
    }

    public RoleInitVo getRole() {
        return role;
    }

    public void setRole(RoleInitVo role) {
        this.role = role;
    }

    public DepartmentInitVo getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentInitVo department) {
        this.department = department;
    }


    
}

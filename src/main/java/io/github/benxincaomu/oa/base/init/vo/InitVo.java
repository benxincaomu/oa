package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class InitVo {

    /**
     * 管理员用户
     */
    @Valid
    @NotNull(message = "管理员用户信息不能为空")
    private UserInitVo user;

    /**
     * 顶级部门名称。全新安装时必填；存量安装（已有租户、只补 Keycloak 链接）时忽略。
     */
    private DepartmentInitVo department;

    public UserInitVo getUser() {
        return user;
    }

    public void setUser(UserInitVo user) {
        this.user = user;
    }

    public DepartmentInitVo getDepartment() {
        return department;
    }

    public void setDepartment(DepartmentInitVo department) {
        this.department = department;
    }

}

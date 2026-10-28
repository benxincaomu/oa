package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class InitVo {

    /**
     * 管理员用户
     */
    @NotNull(message = "管理员用户信息不能为空")
    private UserInitVo user;

    /**
     * 顶级部门名称。全新安装时必填；存量安装（已有租户、只补 Keycloak 配置）时忽略。
     */
    private DepartmentInitVo department;

    @Valid
    @NotNull(message = "Keycloak OAuth2/IAM 配置不能为空")
    private KeycloakConfigVo keycloak;

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

    public KeycloakConfigVo getKeycloak() {
        return keycloak;
    }

    public void setKeycloak(KeycloakConfigVo keycloak) {
        this.keycloak = keycloak;
    }



}

package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.constraints.NotNull;

public class RoleInitVo {

    @NotNull(message = "角色名称不能为空")
    private String roleName;


    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    
}

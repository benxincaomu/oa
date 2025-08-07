package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.constraints.NotNull;

public class RoleInitVo {

    @NotNull(message = "角色名称不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


   

    
}

package io.github.benxincaomu.oa.base.init.vo;


import jakarta.validation.constraints.NotNull;

public class DepartmentInitVo {

    @NotNull(message = "部门名称不能为空")
    private String name;

    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    
}

package io.github.benxincaomu.oa.bussiness.user.vo;


import jakarta.validation.constraints.NotNull;

public class SetPasswordVo {
    
    @NotNull(message = "修改密码标识不能为空")
    private String id;
    @NotNull(message = "新密码不能为空")
    private String password;
    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    
}

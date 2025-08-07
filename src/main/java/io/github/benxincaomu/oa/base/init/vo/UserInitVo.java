package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;

public class UserInitVo {
    @NotNull(message = "用户名不能为空")
    @Max(value = 20,message = "用户名长度不能超过20")
    private String userName;
    @NotNull(message = "密码不能为空")
    private String password;
    @NotNull(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    private String mobile;
    @NotNull(message = "姓名不能为空")
    private String name;
   
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getMobile() {
        return mobile;
    }
    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }

    
}

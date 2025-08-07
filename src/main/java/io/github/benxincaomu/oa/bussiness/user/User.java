package io.github.benxincaomu.oa.bussiness.user;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name ="users",indexes = {@Index(columnList = "userName",name = "users_userName_index")})
@Comment("用户表")
@DynamicUpdate
public class User extends BaseEntity{
    @Comment("用户名")
    @Column(columnDefinition="varchar(20)")
    private String userName;


    @Comment("密码")
    @Column(columnDefinition="varchar(36)")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    
    @Comment("md5 salt")
    @Column(columnDefinition="varchar(10)")
    private String salt;

    @Comment("真实姓名")
    @Column(columnDefinition="varchar(10)")
    private String name;

    @Comment("邮箱")
    @Column(columnDefinition = "varchar(50) ")
    private String email;


    @Comment("手机号")
    @Column(columnDefinition = "varchar(16)")
    private String mobile;

    @Comment("租户id")
    private Long tenantId;


    @Comment("是否启用")
    private Boolean enable;

    @Comment("是否原始密码")
    private Boolean rawPassword;

    @Transient
    private Role role;

    public String getUserName() {
        return userName;
    }


    public void setUserName(String userName) {
        this.userName = userName;
    }


    public String getPassword() {
        return password;
    }


    
    public void setPassword(String password) {
        this.password = password;
    }


    @JsonIgnore
    public String getSalt() {
        return salt;
    }


    public void setSalt(String salt) {
        this.salt = salt;
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public Long getTenantId() {
        return tenantId;
    }


    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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


    public Boolean getEnable() {
        return enable;
    }


    public void setEnable(Boolean enable) {
        this.enable = enable;
    }


    public Role getRole() {
        return role;
    }


    public void setRole(Role role) {
        this.role = role;
    }


    public Boolean getRawPassword() {
        return rawPassword;
    }


    public void setRawPassword(Boolean rawPassword) {
        this.rawPassword = rawPassword;
    }



    
}

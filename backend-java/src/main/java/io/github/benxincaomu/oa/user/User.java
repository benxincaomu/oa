package io.github.benxincaomu.oa.user;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name ="users",indexes = {@Index(columnList = "userName",name = "users_userName_index")})
@Comment("用户表")
public class User extends BaseEntity{
    @Comment("用户名")
    @Column(columnDefinition="varchar(20)")
    private String userName;


    @Comment("密码")
    @Column(columnDefinition="varchar(36)")
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

    
}

package io.github.benxincaomu.oa.base.security;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import io.github.benxincaomu.oa.base.consts.Const;
import io.github.benxincaomu.oa.bussiness.user.Permission;
import io.github.benxincaomu.oa.bussiness.user.Role;

/**
 * token存储结构
 */
@RedisHash(value = Const.TOKEN_KEY_PREFIX, timeToLive = 60 * 60 * 24 * 30)
public class TokenValue implements Serializable {
    private static final long serialVersionUID = -8504023267671780124L;

    @Id
    private String token;
    private Long userId;
    private String userName;
    private Role role;

    private String salt;
    private List<Permission> permissions;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

}

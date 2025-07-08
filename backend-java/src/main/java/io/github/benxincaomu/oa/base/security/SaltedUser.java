package io.github.benxincaomu.oa.base.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class SaltedUser extends User {
    private Long userId;
    private String salt;
    private Long tenantId;

    

    public SaltedUser(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities,Long userId,String salt,Long tenantId) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.salt = salt;
        this.userId = userId;
        this.tenantId = tenantId;
        
    }

    public String getSalt() {
        return salt;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getTenantId() {
        return tenantId;
    }
    

    

    

    


}

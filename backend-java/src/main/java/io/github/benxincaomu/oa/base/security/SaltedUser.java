package io.github.benxincaomu.oa.base.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class SaltedUser extends User {
    private Long userId;
    private String salt;

    

    public SaltedUser(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities,Long userId,String salt) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.salt = salt;
        this.userId = userId;
        
    }

    public String getSalt() {
        return salt;
    }

    public Long getUserId() {
        return userId;
    }
    

    

    

    


}

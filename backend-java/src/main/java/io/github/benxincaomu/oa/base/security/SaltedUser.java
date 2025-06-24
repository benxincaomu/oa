package io.github.benxincaomu.oa.base.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class SaltedUser extends User {
    private io.github.benxincaomu.oa.user.User user;

    public SaltedUser(String username, String password, boolean enabled, boolean accountNonExpired,
            boolean credentialsNonExpired, boolean accountNonLocked,
            Collection<? extends GrantedAuthority> authorities,io.github.benxincaomu.oa.user.User user) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.user = user;
        
    }

    public io.github.benxincaomu.oa.user.User getUser() {
        return user;
    }

    

    


}

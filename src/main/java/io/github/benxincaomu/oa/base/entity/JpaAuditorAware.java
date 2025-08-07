package io.github.benxincaomu.oa.base.entity;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.github.benxincaomu.oa.base.security.SaltedUser;


@Component
public final class JpaAuditorAware  implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return Optional.ofNullable(user.getUserId());
        }
        
        return Optional.empty();
    }

    public static Long getCurrentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return user.getUserId();
        }
        
        return null;
    }
    public static Long getCurrentTenantId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return user.getTenantId();
        }
        
        return null;
    }

    public static String getCurrentRoleId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return user.getUsername();
        }
        
        return null;
    }

}

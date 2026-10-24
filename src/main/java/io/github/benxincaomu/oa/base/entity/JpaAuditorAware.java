package io.github.benxincaomu.oa.base.entity;

import java.util.Optional;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import io.github.benxincaomu.oa.base.security.OaOidcUserPrincipal;
import io.github.benxincaomu.oa.base.security.SaltedUser;


@Component
public final class JpaAuditorAware  implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        Object principal = currentPrincipal();
        if (principal instanceof OaOidcUserPrincipal user) {
            return Optional.ofNullable(user.getOaUserId());
        }
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return Optional.ofNullable(user.getUserId());
        }
        
        return Optional.empty();
    }

    public static Long getCurrentUserId() {
        Object principal = currentPrincipal();
        if (principal instanceof OaOidcUserPrincipal user) {
            return user.getOaUserId();
        }
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return user.getUserId();
        }
        
        return null;
    }
    public static Long getCurrentTenantId() {
        Object principal = currentPrincipal();
        if (principal instanceof OaOidcUserPrincipal user) {
            return user.getTenantId();
        }
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return user.getTenantId();
        }
        
        return null;
    }

    public static String getCurrentRoleId() {
        Object principal = currentPrincipal();
        if (principal instanceof SaltedUser) {
            SaltedUser user = (SaltedUser) principal;
            return user.getUsername();
        }
        
        return null;
    }

    private static Object currentPrincipal() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getPrincipal();
    }

}

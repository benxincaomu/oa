package io.github.benxincaomu.oa.base.security;

import java.io.Serial;

import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import io.github.benxincaomu.oa.bussiness.user.User;

public class OaOidcUserPrincipal extends DefaultOidcUser {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Long oaUserId;
    private final Long tenantId;
    private final String userName;
    private final String displayName;
    private final String email;

    public OaOidcUserPrincipal(OidcUser oidcUser, User localUser, String userName, String displayName,
            String email) {
        super(oidcUser.getAuthorities(), oidcUser.getIdToken(), oidcUser.getUserInfo(), "sub");
        this.oaUserId = localUser.getId();
        this.tenantId = localUser.getTenantId();
        this.userName = userName;
        this.displayName = displayName;
        this.email = email;
    }

    public Long getOaUserId() {
        return oaUserId;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public String getUserName() {
        return userName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }
}

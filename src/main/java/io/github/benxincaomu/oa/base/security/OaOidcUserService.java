package io.github.benxincaomu.oa.base.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.benxincaomu.oa.bussiness.user.User;

/**
 * Builds the OA principal from the OIDC subject and the Keycloak directory.
 *
 * <p>
 * A Keycloak user no longer needs a pre-existing local OA row: the tenant is
 * resolved from the user's Keycloak group hierarchy and the numeric OA ID is
 * allocated by {@link OaNumericIdService} on first login. Keycloak-created users
 * therefore work as soon as they are placed in a tenant group.
 * </p>
 */
@Service
public class OaOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OidcUserService delegate = new OidcUserService();
    private final KeycloakDirectoryService directoryService;
    private final OaNumericIdService numericIdService;

    public OaOidcUserService(KeycloakDirectoryService directoryService, OaNumericIdService numericIdService) {
        this.directoryService = directoryService;
        this.numericIdService = numericIdService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String subject = oidcUser.getSubject();
        Long tenantId = directoryService.resolveTenantIdForUser(subject);
        if (tenantId == null) {
            throw denied("Keycloak 用户尚未分配到任何 OA 租户组");
        }
        KeycloakUserProfile profile = new KeycloakUserProfile(
                subject,
                username(oidcUser),
                displayName(oidcUser),
                oidcUser.getEmail(),
                oidcUser.getClaimAsString("mobile"),
                true);
        User localUser = numericIdService.resolveUser(profile, tenantId);
        if (Boolean.FALSE.equals(localUser.getEnable())) {
            throw denied("OA 用户已停用");
        }
        return new OaOidcUserPrincipal(oidcUser, localUser);
    }

    private String username(OidcUser oidcUser) {
        String username = oidcUser.getPreferredUsername();
        if (!StringUtils.hasText(username)) {
            username = oidcUser.getName();
        }
        return StringUtils.hasText(username) ? username : oidcUser.getSubject();
    }

    private String displayName(OidcUser oidcUser) {
        String fullName = oidcUser.getFullName();
        return StringUtils.hasText(fullName) ? fullName : username(oidcUser);
    }

    private OAuth2AuthenticationException denied(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("access_denied", message, null));
    }
}

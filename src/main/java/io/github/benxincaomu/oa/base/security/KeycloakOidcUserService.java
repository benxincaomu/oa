package io.github.benxincaomu.oa.base.security;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;

@Service
public class KeycloakOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {
    private final OidcUserService delegate = new OidcUserService();
    private final KeycloakObjectLinkRepository objectLinkRepository;
    private final UserRepository userRepository;

    public KeycloakOidcUserService(KeycloakObjectLinkRepository objectLinkRepository, UserRepository userRepository) {
        this.objectLinkRepository = objectLinkRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        KeycloakObjectLink link = objectLinkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.USER, oidcUser.getSubject())
                .orElseThrow(() -> denied("Keycloak 用户尚未关联到 OA 用户"));

        User localUser = userRepository.findById(link.getLocalId())
                .orElseThrow(() -> denied("关联的 OA 用户不存在"));
        if (Boolean.FALSE.equals(localUser.getEnable())) {
            throw denied("OA 用户已停用");
        }
        return new OaOidcUserPrincipal(oidcUser, localUser);
    }

    private OAuth2AuthenticationException denied(String message) {
        return new OAuth2AuthenticationException(new OAuth2Error("access_denied", message, null));
    }
}

package io.github.benxincaomu.oa.base.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Fail-closed request authorization backed by Keycloak Authorization Services.
 *
 * <p>
 * Every non-exempt request is matched against the {@code oa-api} resources
 * registered in Keycloak and then checked with a UMA decision for the logged-in
 * user. A request whose path has no registered resource is denied; a Keycloak
 * error also denies. Exempt paths are limited to login/self-service, file access,
 * and read-only directory pickers needed by workflow forms.
 * </p>
 */
@Component
public class KeycloakRequestAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private static final String[] EXEMPT_PATTERNS = {
            "/error",
            "/actuator/health",
            "/initProject/**",
            "/oauth2/**",
            "/login/oauth2/**",
            "/login/myInfo",
            "/login/getMyMenus",
            "/login/logout",
            "/file/**",
            "/user/currUser",
            "/user/getAllUsers",
            "/user/findUsersByNameLike/**",
            "/organize/listTree",
            "/organize/listAll",
            "/organize/getUsersByDeptId/**"
    };

    private static final long RESOURCE_CACHE_SECONDS = 60;

    private final KeycloakAuthorizationService authorizationService;
    private final KeycloakUamDecisionService decisionService;
    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private volatile CachedResources cachedResources;

    public KeycloakRequestAuthorizationManager(KeycloakAuthorizationService authorizationService,
            KeycloakUamDecisionService decisionService,
            OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizationService = authorizationService;
        this.decisionService = decisionService;
        this.authorizedClientManager = authorizedClientManager;
    }

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authenticationSupplier,
            RequestAuthorizationContext context) {
        HttpServletRequest request = context.getRequest();
        String path = request.getServletPath();
        Authentication authentication = authenticationSupplier.get();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new AuthorizationDecision(false);
        }
        for (String pattern : EXEMPT_PATTERNS) {
            if (matcher.match(pattern, path)) {
                return new AuthorizationDecision(true);
            }
        }
        if (!(authentication instanceof OAuth2AuthenticationToken oauthAuthentication)) {
            return new AuthorizationDecision(false);
        }
        OaStoredResource resource = findResource(path);
        if (resource == null) {
            logger.warn("No Keycloak authorization resource registered for {}", path);
            return new AuthorizationDecision(false);
        }
        String accessToken = accessToken(authentication, oauthAuthentication);
        if (!StringUtils.hasText(accessToken)) {
            return new AuthorizationDecision(false);
        }
        try {
            return new AuthorizationDecision(decisionService.isGranted(resource.key(), accessToken));
        } catch (RuntimeException exception) {
            logger.warn("Keycloak authorization failed for {}; denying request", path, exception);
            return new AuthorizationDecision(false);
        }
    }

    private OaStoredResource findResource(String path) {
        for (OaStoredResource resource : apiResources()) {
            if (resource.value() != null && matcher.match(resource.value(), path)) {
                return resource;
            }
        }
        return null;
    }

    private List<OaStoredResource> apiResources() {
        CachedResources current = cachedResources;
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.resources();
        }
        List<OaStoredResource> resources = authorizationService.listApiResources().stream()
                .filter(resource -> StringUtils.hasText(resource.value()))
                .sorted(Comparator.comparingInt((OaStoredResource resource) -> resource.value().length()).reversed())
                .toList();
        cachedResources = new CachedResources(resources, Instant.now().plusSeconds(RESOURCE_CACHE_SECONDS));
        return resources;
    }

    private String accessToken(Authentication authentication,
            OAuth2AuthenticationToken oauthAuthentication) {
        try {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(oauthAuthentication.getAuthorizedClientRegistrationId())
                    .principal(authentication)
                    .build();
            OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);
            return authorizedClient == null ? null : authorizedClient.getAccessToken().getTokenValue();
        } catch (RuntimeException exception) {
            logger.warn("Unable to obtain a Keycloak access token for the request", exception);
            return null;
        }
    }

    private record CachedResources(List<OaStoredResource> resources, Instant expiresAt) {
    }
}

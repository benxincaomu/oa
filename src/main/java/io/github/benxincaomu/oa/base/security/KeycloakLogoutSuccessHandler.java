package io.github.benxincaomu.oa.base.security;

import java.io.IOException;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {
    private final KeycloakClientRegistrationRepository registrationRepository;
    private final KeycloakConnectionService connectionService;
    private final ObjectMapper objectMapper;

    public KeycloakLogoutSuccessHandler(KeycloakClientRegistrationRepository registrationRepository,
            KeycloakConnectionService connectionService, ObjectMapper objectMapper) {
        this.registrationRepository = registrationRepository;
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        String redirectUri = connectionService.getConfiguration()
                .map(KeycloakIntegrationConfig::getPostLogoutRedirectUri)
                .orElse("/");

        if (authentication instanceof OAuth2AuthenticationToken oauthAuthentication
                && authentication.getPrincipal() instanceof OidcUser oidcUser) {
            ClientRegistration registration = registrationRepository
                    .findByRegistrationId(oauthAuthentication.getAuthorizedClientRegistrationId());
            if (registration != null) {
                Object logoutEndpoint = registration.getProviderDetails().getConfigurationMetadata()
                        .get("end_session_endpoint");
                if (logoutEndpoint instanceof String endpoint && !endpoint.isBlank()) {
                    String logoutUrl = UriComponentsBuilder.fromUriString(endpoint)
                            .queryParam("id_token_hint", oidcUser.getIdToken().getTokenValue())
                            .queryParam("post_logout_redirect_uri", redirectUri)
                            .build()
                            .encode()
                            .toUriString();
                    completeLogout(request, response, logoutUrl);
                    return;
                }
            }
        }
        completeLogout(request, response, redirectUri);
    }

    private void completeLogout(HttpServletRequest request, HttpServletResponse response, String redirectUri)
            throws IOException {
        if ("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json;charset=UTF-8");
            objectMapper.writeValue(response.getWriter(), Map.of(
                    "code", 200,
                    "data", Map.of("redirectUrl", redirectUri)));
            return;
        }
        response.sendRedirect(redirectUri);
    }
}

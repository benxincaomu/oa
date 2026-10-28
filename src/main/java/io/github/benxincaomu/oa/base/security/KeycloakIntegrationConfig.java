package io.github.benxincaomu.oa.base.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Keycloak connection settings, bound from {@code app.keycloak.*} in the
 * application configuration (or the matching environment variables).
 *
 * <p>
 * Settings are no longer stored in the database: the deployment is expected to
 * provide them through {@code application.yml} / profile-specific configuration or
 * environment variables. Secrets are kept in memory only and must not be logged.
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakIntegrationConfig {
    private String issuerUri;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String serviceAccountClientId;
    private String serviceAccountClientSecret;
    private String redirectUri;
    private String postLogoutRedirectUri;

    /**
     * Whether all fields required to run OIDC login and Admin REST calls are set.
     */
    public boolean isConfigured() {
        return StringUtils.hasText(issuerUri)
                && StringUtils.hasText(realm)
                && StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(serviceAccountClientId)
                && StringUtils.hasText(serviceAccountClientSecret)
                && StringUtils.hasText(redirectUri)
                && StringUtils.hasText(postLogoutRedirectUri);
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public void setIssuerUri(String issuerUri) {
        this.issuerUri = issuerUri;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getServiceAccountClientId() {
        return serviceAccountClientId;
    }

    public void setServiceAccountClientId(String serviceAccountClientId) {
        this.serviceAccountClientId = serviceAccountClientId;
    }

    public String getServiceAccountClientSecret() {
        return serviceAccountClientSecret;
    }

    public void setServiceAccountClientSecret(String serviceAccountClientSecret) {
        this.serviceAccountClientSecret = serviceAccountClientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }

    public void setPostLogoutRedirectUri(String postLogoutRedirectUri) {
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }
}

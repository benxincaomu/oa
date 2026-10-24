package io.github.benxincaomu.oa.base.security;

public class KeycloakConnectionStatusVo {
    private final boolean configured;
    private final String issuerUri;
    private final String realm;
    private final String clientId;
    private final String redirectUri;
    private final String postLogoutRedirectUri;

    public KeycloakConnectionStatusVo(boolean configured, String issuerUri, String realm, String clientId,
            String redirectUri, String postLogoutRedirectUri) {
        this.configured = configured;
        this.issuerUri = issuerUri;
        this.realm = realm;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public String getRealm() {
        return realm;
    }

    public String getClientId() {
        return clientId;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getPostLogoutRedirectUri() {
        return postLogoutRedirectUri;
    }
}

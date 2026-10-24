package io.github.benxincaomu.oa.base.init.vo;

import jakarta.validation.constraints.NotBlank;

public class KeycloakConfigVo {
    @NotBlank(message = "Keycloak issuer URI 不能为空")
    private String issuerUri;

    @NotBlank(message = "Realm 不能为空")
    private String realm;

    @NotBlank(message = "OAuth2 客户端 ID 不能为空")
    private String clientId;

    @NotBlank(message = "OAuth2 客户端密钥不能为空")
    private String clientSecret;

    @NotBlank(message = "Keycloak 管理客户端 ID 不能为空")
    private String serviceAccountClientId;

    @NotBlank(message = "Keycloak 管理客户端密钥不能为空")
    private String serviceAccountClientSecret;

    @NotBlank(message = "OAuth2 回调地址不能为空")
    private String redirectUri;

    @NotBlank(message = "退出登录回调地址不能为空")
    private String postLogoutRedirectUri;

    @NotBlank
    private String setupToken;

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

    public String getSetupToken() {
        return setupToken;
    }

    public void setSetupToken(String setupToken) {
        this.setupToken = setupToken;
    }
}

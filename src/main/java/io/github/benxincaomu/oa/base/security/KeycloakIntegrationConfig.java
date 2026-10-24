package io.github.benxincaomu.oa.base.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "keycloak_integration_config")
public class KeycloakIntegrationConfig {
    public static final long CONFIG_ID = 1L;

    @Id
    private Long id = CONFIG_ID;

    @Column(nullable = false, length = 512)
    private String issuerUri;

    @Column(nullable = false, length = 128)
    private String realm;

    @Column(nullable = false, length = 256)
    private String clientId;

    @Column(nullable = false, length = 1024)
    private String encryptedClientSecret;

    @Column(nullable = false, length = 256)
    private String serviceAccountClientId;

    @Column(nullable = false, length = 1024)
    private String encryptedServiceAccountClientSecret;

    @Column(nullable = false, length = 512)
    private String redirectUri;

    @Column(nullable = false, length = 512)
    private String postLogoutRedirectUri;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getEncryptedClientSecret() {
        return encryptedClientSecret;
    }

    public void setEncryptedClientSecret(String encryptedClientSecret) {
        this.encryptedClientSecret = encryptedClientSecret;
    }

    public String getServiceAccountClientId() {
        return serviceAccountClientId;
    }

    public void setServiceAccountClientId(String serviceAccountClientId) {
        this.serviceAccountClientId = serviceAccountClientId;
    }

    public String getEncryptedServiceAccountClientSecret() {
        return encryptedServiceAccountClientSecret;
    }

    public void setEncryptedServiceAccountClientSecret(String encryptedServiceAccountClientSecret) {
        this.encryptedServiceAccountClientSecret = encryptedServiceAccountClientSecret;
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

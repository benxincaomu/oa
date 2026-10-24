package io.github.benxincaomu.oa.base.security;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.stereotype.Component;

@Component
public class KeycloakClientRegistrationRepository implements ClientRegistrationRepository {
    public static final String REGISTRATION_ID = "keycloak";

    private final KeycloakConnectionService connectionService;
    private volatile String cacheKey;
    private volatile ClientRegistration cachedRegistration;

    public KeycloakClientRegistrationRepository(KeycloakConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!REGISTRATION_ID.equals(registrationId)) {
            return null;
        }

        KeycloakIntegrationConfig config = connectionService.getConfiguration().orElse(null);
        if (config == null) {
            return null;
        }

        String currentCacheKey = config.getIssuerUri() + "|" + config.getClientId() + "|"
                + config.getEncryptedClientSecret() + "|" + config.getRedirectUri();
        ClientRegistration registration = cachedRegistration;
        if (registration != null && currentCacheKey.equals(cacheKey)) {
            return registration;
        }

        synchronized (this) {
            if (cachedRegistration != null && currentCacheKey.equals(cacheKey)) {
                return cachedRegistration;
            }
            ClientRegistration.Builder builder = ClientRegistrations.fromIssuerLocation(config.getIssuerUri())
                    .registrationId(REGISTRATION_ID)
                    .clientId(config.getClientId())
                    .clientSecret(connectionService.decryptClientSecret(config))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri(config.getRedirectUri())
                    .scope("openid", "profile", "email")
                    .clientName("OA");
            cachedRegistration = builder.build();
            cacheKey = currentCacheKey;
            return cachedRegistration;
        }
    }
}

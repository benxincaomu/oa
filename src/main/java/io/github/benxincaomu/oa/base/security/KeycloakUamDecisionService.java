package io.github.benxincaomu.oa.base.security;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Requests UMA authorization decisions from Keycloak with the logged-in user's
 * access token (fail closed).
 *
 * <p>
 * A granted decision is cached briefly to avoid one Keycloak round trip per
 * request; a denied decision is not cached because a role grant can change at any
 * time.
 * </p>
 */
@Service
public class KeycloakUamDecisionService {
    private static final long GRANT_CACHE_SECONDS = 30;
    private static final long TOKEN_ENDPOINT_CACHE_SECONDS = 600;

    private final KeycloakConnectionService connectionService;
    private final RestClient restClient;
    private final Map<String, CachedGrant> grants = new ConcurrentHashMap<>();
    private volatile CachedEndpoint cachedEndpoint;

    public KeycloakUamDecisionService(KeycloakConnectionService connectionService,
            RestClient.Builder restClientBuilder) {
        this.connectionService = connectionService;
        this.restClient = restClientBuilder.build();
    }

    public boolean isGranted(String resourceName, String accessToken) {
        if (resourceName == null || resourceName.isBlank() || accessToken == null || accessToken.isBlank()) {
            return false;
        }
        String cacheKey = resourceName + "|" + Integer.toHexString(accessToken.hashCode());
        CachedGrant cached = grants.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return true;
        }
        boolean granted = requestDecision(resourceName, accessToken);
        if (granted) {
            grants.put(cacheKey, new CachedGrant(Instant.now().plusSeconds(GRANT_CACHE_SECONDS)));
            if (grants.size() > 1024) {
                Instant now = Instant.now();
                grants.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
            }
        }
        return granted;
    }

    private boolean requestDecision(String resourceName, String accessToken) {
        KeycloakIntegrationConfig config = connectionService.getConfiguration()
                .orElseThrow(() -> new IllegalStateException("Keycloak is not configured"));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "urn:ietf:params:oauth:grant-type:uma-ticket");
        form.add("audience", config.getClientId());
        form.add("permission", resourceName);
        form.add("response_mode", "decision");
        try {
            JsonNode response = restClient.post()
                    .uri(tokenEndpoint(config))
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            return response != null && response.path("result").asBoolean(false);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 403 || exception.getStatusCode().value() == 401) {
                return false;
            }
            throw new IllegalStateException("Keycloak UMA authorization decision failed", exception);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Keycloak UMA authorization decision failed", exception);
        }
    }

    private String tokenEndpoint(KeycloakIntegrationConfig config) {
        String configurationKey = config.getIssuerUri() + "|" + config.getRealm();
        CachedEndpoint current = cachedEndpoint;
        if (current != null && current.configurationKey().equals(configurationKey)
                && current.expiresAt().isAfter(Instant.now())) {
            return current.endpoint();
        }
        String endpoint = connectionService.getTokenEndpoint(config);
        URI parsed = URI.create(endpoint);
        if (parsed.getHost() == null) {
            throw new IllegalStateException("Keycloak token endpoint is invalid");
        }
        cachedEndpoint = new CachedEndpoint(configurationKey, endpoint,
                Instant.now().plusSeconds(TOKEN_ENDPOINT_CACHE_SECONDS));
        return endpoint;
    }

    private record CachedGrant(Instant expiresAt) {
    }

    private record CachedEndpoint(String configurationKey, String endpoint, Instant expiresAt) {
    }
}

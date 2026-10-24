package io.github.benxincaomu.oa.base.security;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.benxincaomu.oa.base.init.vo.UserInitVo;

@Service
public class KeycloakAdminClient {
    private final KeycloakConnectionService connectionService;
    private final RestClient restClient;
    private volatile CachedToken cachedToken;

    public KeycloakAdminClient(KeycloakConnectionService connectionService, RestClient.Builder restClientBuilder) {
        this.connectionService = connectionService;
        this.restClient = restClientBuilder.build();
    }

    public KeycloakBootstrapIdentity bootstrapAdministrator(UserInitVo user, String rootGroupName,
            String administratorRoleName) {
        KeycloakIntegrationConfig config = connectionService.getConfiguration()
                .orElseThrow(() -> new IllegalStateException("Keycloak 尚未配置"));
        String adminBase = adminRealmBase(config);

        String userId = findUser(config, user.getUserName())
                .orElseGet(() -> createUser(config, user));
        String groupId = findGroup(config, rootGroupName)
                .orElseGet(() -> createGroup(config, rootGroupName));
        JsonNode role = findRole(config, administratorRoleName)
                .orElseGet(() -> createRole(config, administratorRoleName));

        authorizedPut(config, adminBase + "/users/{userId}/groups/{groupId}", userId, groupId)
                .retrieve()
                .toBodilessEntity();

        authorizedPost(config, adminBase + "/users/{userId}/role-mappings/realm", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();

        return new KeycloakBootstrapIdentity(userId, groupId, role.path("id").asText());
    }

    private Optional<String> findUser(KeycloakIntegrationConfig config, String username) {
        String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/users")
                .queryParam("username", username)
                .queryParam("exact", true)
                .build()
                .encode()
                .toUriString();
        JsonNode users = authorizedGet(config, uri).retrieve().body(JsonNode.class);
        if (users != null && users.isArray()) {
            for (JsonNode user : users) {
                if (username.equals(user.path("username").asText())) {
                    return Optional.of(user.path("id").asText());
                }
            }
        }
        return Optional.empty();
    }

    private String createUser(KeycloakIntegrationConfig config, UserInitVo user) {
        Map<String, Object> payload = Map.of(
                "username", user.getUserName(),
                "enabled", true,
                "email", user.getEmail(),
                "emailVerified", false,
                "firstName", user.getName(),
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", user.getPassword(),
                        "temporary", true)));
        var response = authorizedPost(config, adminRealmBase(config) + "/users")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .toBodilessEntity();
        String id = locationId(response.getHeaders());
        if (id == null) {
            return findUser(config, user.getUserName())
                    .orElseThrow(() -> new IllegalStateException("Keycloak 已创建管理员但未返回用户 ID"));
        }
        return id;
    }

    private Optional<String> findGroup(KeycloakIntegrationConfig config, String name) {
        String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/groups")
                .queryParam("search", name)
                .build()
                .encode()
                .toUriString();
        JsonNode groups = authorizedGet(config, uri).retrieve().body(JsonNode.class);
        if (groups != null && groups.isArray()) {
            for (JsonNode group : groups) {
                if (name.equals(group.path("name").asText())) {
                    return Optional.of(group.path("id").asText());
                }
            }
        }
        return Optional.empty();
    }

    private String createGroup(KeycloakIntegrationConfig config, String name) {
        var response = authorizedPost(config, adminRealmBase(config) + "/groups")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name))
                .retrieve()
                .toBodilessEntity();
        String id = locationId(response.getHeaders());
        if (id == null) {
            return findGroup(config, name)
                    .orElseThrow(() -> new IllegalStateException("Keycloak 已创建组织但未返回组织 ID"));
        }
        return id;
    }

    private Optional<JsonNode> findRole(KeycloakIntegrationConfig config, String name) {
        try {
            return Optional.ofNullable(authorizedGet(config,
                    adminRealmBase(config) + "/roles/{roleName}", name)
                    .retrieve()
                    .body(JsonNode.class));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("无法读取 Keycloak 管理角色", exception);
        }
    }

    private JsonNode createRole(KeycloakIntegrationConfig config, String name) {
        authorizedPost(config, adminRealmBase(config) + "/roles")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("name", name, "description", "OA initial administrator role"))
                .retrieve()
                .toBodilessEntity();
        return findRole(config, name)
                .orElseThrow(() -> new IllegalStateException("Keycloak 已创建管理员角色但无法读取"));
    }

    private RestClient.RequestHeadersSpec<?> authorizedGet(KeycloakIntegrationConfig config, String uri,
            Object... uriVariables) {
        return restClient.get().uri(uri, uriVariables)
                .headers(headers -> headers.setBearerAuth(accessToken(config)));
    }

    private RestClient.RequestBodySpec authorizedPost(KeycloakIntegrationConfig config, String uri,
            Object... uriVariables) {
        return restClient.post().uri(uri, uriVariables)
                .headers(headers -> headers.setBearerAuth(accessToken(config)));
    }

    private RestClient.RequestBodySpec authorizedPut(KeycloakIntegrationConfig config, String uri,
            Object... uriVariables) {
        return restClient.put().uri(uri, uriVariables)
                .headers(headers -> headers.setBearerAuth(accessToken(config)));
    }

    private String accessToken(KeycloakIntegrationConfig config) {
        CachedToken current = cachedToken;
        String configurationKey = tokenCacheKey(config);
        if (current != null && current.configurationKey().equals(configurationKey)
                && current.expiresAt().isAfter(Instant.now().plusSeconds(15))) {
            return current.value();
        }
        synchronized (this) {
            current = cachedToken;
            if (current != null && current.configurationKey().equals(configurationKey)
                    && current.expiresAt().isAfter(Instant.now().plusSeconds(15))) {
                return current.value();
            }
            String tokenEndpoint = connectionService.getTokenEndpoint(config);
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "client_credentials");
            form.add("client_id", config.getServiceAccountClientId());
            form.add("client_secret", connectionService.decryptServiceAccountClientSecret(config));
            JsonNode response;
            try {
                response = restClient.post()
                        .uri(tokenEndpoint)
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(form)
                        .retrieve()
                        .body(JsonNode.class);
            } catch (RestClientException exception) {
                throw new IllegalStateException("无法取得 Keycloak 管理客户端访问令牌", exception);
            }
            String value = response == null ? "" : response.path("access_token").asText();
            long lifetime = response == null ? 0 : response.path("expires_in").asLong();
            if (value.isBlank() || lifetime <= 0) {
                throw new IllegalStateException("Keycloak 管理客户端返回的访问令牌无效");
            }
            cachedToken = new CachedToken(value, Instant.now().plusSeconds(lifetime), configurationKey);
            return value;
        }
    }

    private String adminRealmBase(KeycloakIntegrationConfig config) {
        URI issuer = URI.create(config.getIssuerUri());
        String realmSuffix = "/realms/" + config.getRealm();
        String path = issuer.getPath();
        String serverPath = path.substring(0, path.length() - realmSuffix.length());
        return issuer.getScheme() + "://" + issuer.getRawAuthority() + serverPath + "/admin/realms/"
                + URLEncoder.encode(config.getRealm(), StandardCharsets.UTF_8);
    }

    private String locationId(HttpHeaders headers) {
        URI location = headers.getLocation();
        if (location == null || location.getPath() == null) {
            return null;
        }
        String path = location.getPath();
        int lastSlash = path.lastIndexOf('/');
        return lastSlash < 0 ? null : path.substring(lastSlash + 1);
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String tokenCacheKey(KeycloakIntegrationConfig config) {
        return config.getIssuerUri() + "|" + config.getServiceAccountClientId() + "|"
                + config.getEncryptedServiceAccountClientSecret();
    }

    private record CachedToken(String value, Instant expiresAt, String configurationKey) {
    }
}

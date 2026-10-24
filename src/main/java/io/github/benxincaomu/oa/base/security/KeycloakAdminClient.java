package io.github.benxincaomu.oa.base.security;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.github.benxincaomu.oa.base.init.vo.UserInitVo;

@Service
public class KeycloakAdminClient {
    private static final int PAGE_SIZE = 100;

    private final KeycloakConnectionService connectionService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private volatile CachedToken cachedToken;

    public KeycloakAdminClient(KeycloakConnectionService connectionService, RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.connectionService = connectionService;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
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

    public KeycloakUserProfile upsertUser(KeycloakUserProfile user) {
        String expectedUsernameMatchId = findUserIdByUsername(user.username()).orElse(null);
        return upsertUser(user, expectedUsernameMatchId);
    }

    public KeycloakUserProfile upsertUser(KeycloakUserProfile user, String expectedUsernameMatchId) {
        KeycloakIntegrationConfig config = configured();
        String usernameMatchId = findUser(config, user.username()).orElse(null);
        if (!java.util.Objects.equals(usernameMatchId, expectedUsernameMatchId)
                || (hasText(user.id()) && usernameMatchId != null && !user.id().equals(usernameMatchId))) {
            throw new IllegalStateException("Keycloak user changed after tenant validation");
        }
        String userId = hasText(user.id()) ? user.id() : usernameMatchId;
        ObjectNode payload = userId == null
                ? objectMapper.createObjectNode()
                : readUser(config, userId).deepCopy();

        payload.put("username", user.username());
        payload.put("firstName", user.name() == null ? "" : user.name());
        payload.put("enabled", user.enabled());
        if (hasText(user.email())) {
            payload.put("email", user.email());
        } else {
            payload.remove("email");
        }

        ObjectNode attributes = payload.with("attributes");
        if (hasText(user.mobile())) {
            attributes.putArray("mobile").add(user.mobile());
        } else {
            attributes.remove("mobile");
        }

        if (userId == null) {
            var response = authorizedPost(config, adminRealmBase(config) + "/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            userId = locationId(response.getHeaders());
            if (userId == null) {
                userId = findUser(config, user.username())
                        .orElseThrow(() -> new IllegalStateException("Keycloak created a user without returning its ID"));
            }
        } else {
            authorizedPut(config, adminRealmBase(config) + "/users/{userId}", userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        }
        return new KeycloakUserProfile(userId, user.username(), user.name(), user.email(), user.mobile(), user.enabled());
    }

    public Optional<String> findUserIdByUsername(String username) {
        return findUser(configured(), username);
    }

    public void setUserEnabled(String userId, boolean enabled) {
        KeycloakIntegrationConfig config = configured();
        ObjectNode representation = readUser(config, userId);
        representation.put("enabled", enabled);
        authorizedPut(config, adminRealmBase(config) + "/users/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(representation)
                .retrieve()
                .toBodilessEntity();
    }

    public KeycloakGroupProfile upsertGroup(KeycloakGroupProfile group) {
        String expectedPathMatchId = findGroupId(group.parentId(), group.name()).orElse(null);
        return upsertGroup(group, expectedPathMatchId);
    }

    public KeycloakGroupProfile upsertGroup(KeycloakGroupProfile group, String expectedPathMatchId) {
        KeycloakIntegrationConfig config = configured();
        String parentId = hasText(group.parentId()) ? group.parentId() : null;
        String pathMatchId = findGroup(config, parentId, group.name()).orElse(null);
        if (!java.util.Objects.equals(pathMatchId, expectedPathMatchId)
                || (hasText(group.id()) && pathMatchId != null && !group.id().equals(pathMatchId))) {
            throw new IllegalStateException("Keycloak group changed after tenant validation");
        }
        String groupId = hasText(group.id()) ? group.id() : pathMatchId;

        if (groupId == null) {
            ObjectNode payload = objectMapper.createObjectNode().put("name", group.name());
            setGroupAttributes(payload, group);
            String endpoint = parentId == null
                    ? adminRealmBase(config) + "/groups"
                    : adminRealmBase(config) + "/groups/{parentId}/children";
            var response = parentId == null
                    ? authorizedPost(config, endpoint).contentType(MediaType.APPLICATION_JSON).body(payload)
                            .retrieve().toBodilessEntity()
                    : authorizedPost(config, endpoint, parentId).contentType(MediaType.APPLICATION_JSON)
                            .body(payload).retrieve().toBodilessEntity();
            groupId = locationId(response.getHeaders());
            if (groupId == null) {
                groupId = findGroup(config, parentId, group.name())
                        .orElseThrow(() -> new IllegalStateException("Keycloak created a group without returning its ID"));
            }
        } else {
            ObjectNode payload = readGroup(config, groupId);
            ensureParentUnchanged(config, payload, parentId);
            payload.put("name", group.name());
            setGroupAttributes(payload, group);
            authorizedPut(config, adminRealmBase(config) + "/groups/{groupId}", groupId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        }
        return new KeycloakGroupProfile(groupId, group.name(), group.code(), parentId);
    }

    public Optional<String> findGroupId(String parentId, String name) {
        return findGroup(configured(), hasText(parentId) ? parentId : null, name);
    }

    public void setGroupMembership(String userId, String groupId, boolean member) {
        KeycloakIntegrationConfig config = configured();
        String endpoint = adminRealmBase(config) + "/users/{userId}/groups/{groupId}";
        if (member) {
            authorizedPut(config, endpoint, userId, groupId).retrieve().toBodilessEntity();
        } else {
            authorizedDelete(config, endpoint, userId, groupId).retrieve().toBodilessEntity();
        }
    }

    public List<String> getGroupMemberIds(String groupId) {
        KeycloakIntegrationConfig config = configured();
        List<String> memberIds = new ArrayList<>();
        int first = 0;
        while (true) {
            String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/groups/{groupId}/members")
                    .queryParam("first", first)
                    .queryParam("max", PAGE_SIZE)
                    .buildAndExpand(groupId)
                    .encode()
                    .toUriString();
            JsonNode members = authorizedGet(config, uri).retrieve().body(JsonNode.class);
            if (members == null || !members.isArray()) {
                break;
            }
            for (JsonNode member : members) {
                String memberId = member.path("id").asText();
                if (hasText(memberId)) {
                    memberIds.add(memberId);
                }
            }
            if (members.size() < PAGE_SIZE) {
                break;
            }
            first += PAGE_SIZE;
        }
        return memberIds;
    }

    public List<String> getEffectiveUserClientRoleIds(String userId) {
        KeycloakIntegrationConfig config = configured();
        String clientUuid = clientUuid(config);
        String endpoint = adminRealmBase(config) + "/users/{userId}/role-mappings/clients/{clientUuid}/composite";
        return getRoleIds(config, endpoint, userId, clientUuid);
    }

    public List<String> getEffectiveUserRoleIds(String userId) {
        KeycloakIntegrationConfig config = configured();
        Set<String> roleIds = new LinkedHashSet<>();
        roleIds.addAll(getRoleIds(config,
                adminRealmBase(config) + "/users/{userId}/role-mappings/realm/composite", userId));
        String clientUuid = clientUuid(config);
        roleIds.addAll(getRoleIds(config,
                adminRealmBase(config) + "/users/{userId}/role-mappings/clients/{clientUuid}/composite",
                userId, clientUuid));
        return List.copyOf(roleIds);
    }

    public void replaceUserClientRoles(String userId, Collection<String> roleIds) {
        KeycloakIntegrationConfig config = configured();
        String clientUuid = clientUuid(config);
        String mappingsEndpoint = adminRealmBase(config) + "/users/{userId}/role-mappings/clients/{clientUuid}";
        JsonNode currentRolesNode = authorizedGet(config, mappingsEndpoint, userId, clientUuid)
                .retrieve().body(JsonNode.class);
        List<JsonNode> currentRoles = arrayValues(currentRolesNode);
        Set<String> desiredIds = new LinkedHashSet<>(roleIds == null ? List.of() : roleIds);
        List<JsonNode> remove = currentRoles.stream()
                .filter(role -> !desiredIds.contains(role.path("id").asText()))
                .toList();
        Set<String> currentIds = new LinkedHashSet<>();
        currentRoles.forEach(role -> currentIds.add(role.path("id").asText()));
        List<JsonNode> add = new ArrayList<>();
        for (String roleId : desiredIds) {
            if (!hasText(roleId)) {
                throw new IllegalArgumentException("Keycloak role ID cannot be blank");
            }
            if (!currentIds.contains(roleId)) {
                JsonNode role = authorizedGet(config, adminRealmBase(config) + "/roles-by-id/{roleId}", roleId)
                        .retrieve().body(JsonNode.class);
                if (role == null || !role.path("clientRole").asBoolean()
                        || !clientUuid.equals(role.path("containerId").asText())) {
                    throw new IllegalArgumentException("Keycloak role does not belong to the configured application client");
                }
                add.add(role);
            }
        }
        if (!remove.isEmpty()) {
            authorizedDelete(config, mappingsEndpoint, userId, clientUuid)
                    .contentType(MediaType.APPLICATION_JSON).body(remove).retrieve().toBodilessEntity();
        }
        if (!add.isEmpty()) {
            authorizedPost(config, mappingsEndpoint, userId, clientUuid)
                    .contentType(MediaType.APPLICATION_JSON).body(add).retrieve().toBodilessEntity();
        }
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

    private ObjectNode readUser(KeycloakIntegrationConfig config, String userId) {
        JsonNode user = authorizedGet(config, adminRealmBase(config) + "/users/{userId}", userId)
                .retrieve().body(JsonNode.class);
        if (user instanceof ObjectNode object) {
            return object;
        }
        throw new IllegalStateException("Keycloak user representation is missing");
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
        return findGroup(config, null, name);
    }

    private Optional<String> findGroup(KeycloakIntegrationConfig config, String parentId, String name) {
        String path = parentId == null ? "/groups" : "/groups/{parentId}/children";
        int first = 0;
        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(adminRealmBase(config) + path)
                    .queryParam("search", name)
                    .queryParam("exact", true)
                    .queryParam("first", first)
                    .queryParam("max", PAGE_SIZE);
            String uri = parentId == null
                    ? builder.build().encode().toUriString()
                    : builder.buildAndExpand(parentId).encode().toUriString();
            JsonNode groups = authorizedGet(config, uri).retrieve().body(JsonNode.class);
            if (groups == null || !groups.isArray() || groups.isEmpty()) {
                return Optional.empty();
            }
            for (JsonNode group : groups) {
                if (name.equals(group.path("name").asText())) {
                    return Optional.of(group.path("id").asText());
                }
            }
            if (groups.size() < PAGE_SIZE) {
                return Optional.empty();
            }
            first += PAGE_SIZE;
        }
    }

    private ObjectNode readGroup(KeycloakIntegrationConfig config, String groupId) {
        JsonNode group = authorizedGet(config, adminRealmBase(config) + "/groups/{groupId}", groupId)
                .retrieve().body(JsonNode.class);
        if (group instanceof ObjectNode object) {
            return object;
        }
        throw new IllegalStateException("Keycloak group representation is missing");
    }

    private void setGroupAttributes(ObjectNode groupNode, KeycloakGroupProfile group) {
        ObjectNode attributes = groupNode.with("attributes");
        if (hasText(group.code())) {
            attributes.putArray("code").add(group.code());
        } else {
            attributes.remove("code");
        }
    }

    private void ensureParentUnchanged(KeycloakIntegrationConfig config, ObjectNode group, String requestedParentId) {
        String path = group.path("path").asText();
        int lastSlash = path.lastIndexOf('/');
        String actualParentPath = lastSlash <= 0 ? "" : path.substring(0, lastSlash);
        String requestedParentPath = requestedParentId == null
                ? ""
                : readGroup(config, requestedParentId).path("path").asText();
        if (!actualParentPath.equals(requestedParentPath)) {
            throw new IllegalArgumentException(
                    "Keycloak group parent cannot be changed through the current synchronization endpoint");
        }
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

    private RestClient.RequestBodySpec authorizedDelete(KeycloakIntegrationConfig config, String uri,
            Object... uriVariables) {
        return restClient.method(org.springframework.http.HttpMethod.DELETE).uri(uri, uriVariables)
                .headers(headers -> headers.setBearerAuth(accessToken(config)));
    }

    private KeycloakIntegrationConfig configured() {
        return connectionService.getConfiguration()
                .orElseThrow(() -> new IllegalStateException("Keycloak is not configured"));
    }

    private String clientUuid(KeycloakIntegrationConfig config) {
        String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/clients")
                .queryParam("clientId", config.getClientId())
                .build().encode().toUriString();
        JsonNode clients = authorizedGet(config, uri).retrieve().body(JsonNode.class);
        if (clients != null && clients.isArray()) {
            for (JsonNode client : clients) {
                if (config.getClientId().equals(client.path("clientId").asText())) {
                    String id = client.path("id").asText();
                    if (hasText(id)) {
                        return id;
                    }
                }
            }
        }
        throw new IllegalStateException("Configured Keycloak application client was not found");
    }

    private List<JsonNode> arrayValues(JsonNode node) {
        List<JsonNode> values = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(values::add);
        }
        return values;
    }

    private List<String> getRoleIds(KeycloakIntegrationConfig config, String endpoint, Object... uriVariables) {
        JsonNode roles = authorizedGet(config, endpoint, uriVariables).retrieve().body(JsonNode.class);
        List<String> ids = new ArrayList<>();
        if (roles != null && roles.isArray()) {
            for (JsonNode role : roles) {
                String id = role.path("id").asText();
                if (hasText(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

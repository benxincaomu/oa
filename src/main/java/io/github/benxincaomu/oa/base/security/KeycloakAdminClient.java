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

    /** Application client role that owns platform administration. */
    public static final String OA_ADMIN_CLIENT_ROLE = "oa-admin";

    private final KeycloakConnectionService connectionService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private volatile CachedToken cachedToken;
    private volatile CachedClientUuid cachedClientUuid;
    private volatile String authorizationServicesConfigurationKey;

    public KeycloakAdminClient(KeycloakConnectionService connectionService, RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper) {
        this.connectionService = connectionService;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public KeycloakBootstrapIdentity bootstrapAdministrator(UserInitVo user, String rootGroupName) {
        KeycloakIntegrationConfig config = connectionService.getConfiguration()
                .orElseThrow(() -> new IllegalStateException("Keycloak 尚未配置"));
        String adminBase = adminRealmBase(config);

        String userId = findUser(config, user.getUserName())
                .orElseGet(() -> createUser(config, user));
        String groupId = findGroup(config, rootGroupName)
                .orElseGet(() -> createGroup(config, rootGroupName));
        String applicationClientUuid = clientUuid(config);
        enableAuthorizationServices(config, applicationClientUuid);
        JsonNode managementRole = findClientRole(config, applicationClientUuid, OA_ADMIN_CLIENT_ROLE)
                .orElseGet(() -> createClientRole(config, applicationClientUuid,
                        new KeycloakRoleProfile(null, OA_ADMIN_CLIENT_ROLE, "OA platform administrator")));

        authorizedPut(config, adminBase + "/users/{userId}/groups/{groupId}", userId, groupId)
                .retrieve()
                .toBodilessEntity();

        authorizedPost(config,
                adminBase + "/users/{userId}/role-mappings/clients/{clientUuid}", userId, applicationClientUuid)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(managementRole))
                .retrieve()
                .toBodilessEntity();

        return new KeycloakBootstrapIdentity(userId, groupId, managementRole.path("id").asText());
    }

    /**
     * Sets one attribute on a Keycloak group (creating the attributes map when
     * missing). Used to mark the tenant root group with the OA tenant ID.
     */
    public void setGroupAttribute(String groupId, String name, String value) {
        KeycloakIntegrationConfig config = configured();
        ObjectNode group = readGroup(config, groupId);
        ObjectNode attributes = group.with("attributes");
        if (hasText(value)) {
            attributes.putArray(name).add(value);
        } else {
            attributes.remove(name);
        }
        authorizedPut(config, adminRealmBase(config) + "/groups/{groupId}", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(group)
                .retrieve()
                .toBodilessEntity();
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

    public List<KeycloakUserProfile> listUsers(String search) {
        KeycloakIntegrationConfig config = configured();
        List<KeycloakUserProfile> users = new ArrayList<>();
        int first = 0;
        while (true) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/users")
                    .queryParam("first", first)
                    .queryParam("max", PAGE_SIZE);
            if (hasText(search)) {
                builder.queryParam("search", search.trim());
            }
            JsonNode page = authorizedGet(config, builder.build().encode().toUriString())
                    .retrieve().body(JsonNode.class);
            if (page == null || !page.isArray() || page.isEmpty()) {
                break;
            }
            for (JsonNode user : page) {
                users.add(userProfile(user));
            }
            if (page.size() < PAGE_SIZE) {
                break;
            }
            first += PAGE_SIZE;
        }
        return List.copyOf(users);
    }

    public Optional<KeycloakUserProfile> findUserProfile(String userId) {
        if (!hasText(userId)) {
            return Optional.empty();
        }
        KeycloakIntegrationConfig config = configured();
        try {
            JsonNode user = authorizedGet(config, adminRealmBase(config) + "/users/{userId}", userId)
                    .retrieve().body(JsonNode.class);
            return user == null || !user.isObject() ? Optional.empty() : Optional.of(userProfile(user));
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("Unable to read Keycloak user", exception);
        }
    }

    private KeycloakUserProfile userProfile(JsonNode user) {
        String firstName = user.path("firstName").asText("");
        String lastName = user.path("lastName").asText("");
        String name = (firstName + " " + lastName).trim();
        return new KeycloakUserProfile(
                user.path("id").asText(),
                user.path("username").asText(),
                name.isEmpty() ? firstName : name,
                user.path("email").asText(null),
                attributeValue(user.path("attributes").path("mobile")),
                user.path("enabled").asBoolean(true));
    }

    public void resetUserPassword(String userId, String password, boolean temporary) {
        if (!hasText(password)) {
            throw new IllegalArgumentException("Password cannot be blank");
        }
        KeycloakIntegrationConfig config = configured();
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", password,
                "temporary", temporary);
        authorizedPut(config, adminRealmBase(config) + "/users/{userId}/reset-password", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(credential)
                .retrieve()
                .toBodilessEntity();
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

    public List<KeycloakGroupProfile> listGroups() {
        KeycloakIntegrationConfig config = configured();
        List<KeycloakGroupProfile> groups = new ArrayList<>();
        int first = 0;
        while (true) {
            String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/groups")
                    .queryParam("first", first)
                    .queryParam("max", PAGE_SIZE)
                    .queryParam("briefRepresentation", false)
                    .build().encode().toUriString();
            JsonNode page = authorizedGet(config, uri).retrieve().body(JsonNode.class);
            if (page == null || !page.isArray() || page.isEmpty()) {
                break;
            }
            appendGroupTree(page, null, groups);
            if (page.size() < PAGE_SIZE) {
                break;
            }
            first += PAGE_SIZE;
        }
        return List.copyOf(groups);
    }

    public List<OaKeycloakGroup> listGroupsWithAttributes() {
        KeycloakIntegrationConfig config = configured();
        List<OaKeycloakGroup> groups = new ArrayList<>();
        int first = 0;
        while (true) {
            String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/groups")
                    .queryParam("first", first)
                    .queryParam("max", PAGE_SIZE)
                    .queryParam("briefRepresentation", false)
                    .build().encode().toUriString();
            JsonNode page = authorizedGet(config, uri).retrieve().body(JsonNode.class);
            if (page == null || !page.isArray() || page.isEmpty()) {
                break;
            }
            appendGroupNodes(page, null, groups);
            if (page.size() < PAGE_SIZE) {
                break;
            }
            first += PAGE_SIZE;
        }
        return List.copyOf(groups);
    }

    public List<OaKeycloakGroup> listUserGroups(String userId) {
        KeycloakIntegrationConfig config = configured();
        List<OaKeycloakGroup> groups = new ArrayList<>();
        int first = 0;
        while (true) {
            String uri = UriComponentsBuilder
                    .fromUriString(adminRealmBase(config) + "/users/{userId}/groups")
                    .queryParam("first", first)
                    .queryParam("max", PAGE_SIZE)
                    .queryParam("briefRepresentation", false)
                    .buildAndExpand(userId)
                    .encode()
                    .toUriString();
            JsonNode page = authorizedGet(config, uri).retrieve().body(JsonNode.class);
            if (page == null || !page.isArray() || page.isEmpty()) {
                break;
            }
            for (JsonNode group : page) {
                groups.add(groupNode(group, null));
            }
            if (page.size() < PAGE_SIZE) {
                break;
            }
            first += PAGE_SIZE;
        }
        return List.copyOf(groups);
    }

    private void appendGroupNodes(JsonNode groups, String parentId, List<OaKeycloakGroup> result) {
        for (JsonNode group : groups) {
            OaKeycloakGroup node = groupNode(group, parentId);
            result.add(node);
            JsonNode children = group.path("subGroups");
            if (children.isArray() && !children.isEmpty()) {
                appendGroupNodes(children, node.id(), result);
            }
        }
    }

    private OaKeycloakGroup groupNode(JsonNode group, String parentId) {
        String name = group.path("name").asText();
        String path = group.path("path").asText();
        if (!hasText(path)) {
            path = "/" + name;
        }
        JsonNode attributes = group.path("attributes");
        return new OaKeycloakGroup(
                group.path("id").asText(),
                name,
                path,
                parentId,
                attributeValue(attributes.path("code")),
                parseLong(attributeValue(attributes.path("tenant_id"))),
                parseLong(attributeValue(attributes.path("leader_user_id"))));
    }

    private Long parseLong(String value) {
        if (!hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public KeycloakRoleProfile upsertClientRole(KeycloakRoleProfile requested) {
        KeycloakIntegrationConfig config = configured();
        String clientId = clientUuid(config);
        JsonNode existing;
        String endpoint;
        if (hasText(requested.id())) {
            existing = readClientRoleById(config, clientId, requested.id());
            endpoint = adminRealmBase(config) + "/roles-by-id/{roleId}";
        } else {
            existing = findClientRole(config, clientId, requested.name()).orElse(null);
            endpoint = adminRealmBase(config) + "/clients/{clientId}/roles/{roleName}";
        }
        ObjectNode representation = existing instanceof ObjectNode object ? object.deepCopy()
                : objectMapper.createObjectNode();
        representation.put("name", requested.name());
        if (requested.description() != null) {
            representation.put("description", requested.description());
        }
        if (existing == null) {
            authorizedPost(config, adminRealmBase(config) + "/clients/{clientId}/roles", clientId)
                    .contentType(MediaType.APPLICATION_JSON).body(representation).retrieve().toBodilessEntity();
        } else {
            if (hasText(requested.id())) {
                authorizedPut(config, endpoint, requested.id())
                        .contentType(MediaType.APPLICATION_JSON).body(representation).retrieve().toBodilessEntity();
            } else {
                authorizedPut(config, endpoint, clientId, requested.name())
                        .contentType(MediaType.APPLICATION_JSON).body(representation).retrieve().toBodilessEntity();
            }
        }
        JsonNode saved = hasText(requested.id())
                ? readClientRoleById(config, clientId, requested.id())
                : findClientRole(config, clientId, requested.name())
                        .orElseThrow(() -> new IllegalStateException("Keycloak client role was not saved"));
        return clientRoleProfile(saved);
    }

    public List<KeycloakRoleProfile> listClientRoles() {
        KeycloakIntegrationConfig config = configured();
        String clientId = clientUuid(config);
        List<KeycloakRoleProfile> roles = new ArrayList<>();
        int first = 0;
        while (true) {
            String uri = UriComponentsBuilder.fromUriString(adminRealmBase(config) + "/clients/{clientId}/roles")
                    .queryParam("first", first).queryParam("max", PAGE_SIZE)
                    .buildAndExpand(clientId).encode().toUriString();
            JsonNode page = authorizedGet(config, uri).retrieve().body(JsonNode.class);
            if (page == null || !page.isArray() || page.isEmpty()) {
                break;
            }
            for (JsonNode role : page) {
                roles.add(clientRoleProfile(role));
            }
            if (page.size() < PAGE_SIZE) {
                break;
            }
            first += PAGE_SIZE;
        }
        return List.copyOf(roles);
    }

    public void deleteClientRole(String roleId) {
        KeycloakIntegrationConfig config = configured();
        String clientId = clientUuid(config);
        JsonNode role = authorizedGet(config, adminRealmBase(config) + "/roles-by-id/{roleId}", roleId)
                .retrieve().body(JsonNode.class);
        if (role == null || !clientId.equals(role.path("containerId").asText())) {
            throw new IllegalArgumentException("Keycloak role does not belong to the configured application client");
        }
        authorizedDelete(config,
                adminRealmBase(config) + "/clients/{clientId}/roles/{roleName}",
                clientId, role.path("name").asText())
                .retrieve().toBodilessEntity();
    }

    public void replaceGroupClientRoles(String groupId, Collection<String> roleIds) {
        KeycloakIntegrationConfig config = configured();
        String clientId = clientUuid(config);
        String mappingsEndpoint = adminRealmBase(config) + "/groups/{groupId}/role-mappings/clients/{clientId}";
        JsonNode currentNode = authorizedGet(config, mappingsEndpoint, groupId, clientId)
                .retrieve().body(JsonNode.class);
        List<JsonNode> currentRoles = arrayValues(currentNode);
        Set<String> desiredIds = new LinkedHashSet<>(roleIds == null ? List.of() : roleIds);
        Set<String> currentIds = new LinkedHashSet<>();
        currentRoles.forEach(role -> currentIds.add(role.path("id").asText()));
        List<JsonNode> remove = currentRoles.stream()
                .filter(role -> !desiredIds.contains(role.path("id").asText()))
                .toList();
        List<JsonNode> add = new ArrayList<>();
        for (String roleId : desiredIds) {
            if (!hasText(roleId)) {
                throw new IllegalArgumentException("Keycloak role ID cannot be blank");
            }
            if (!currentIds.contains(roleId)) {
                JsonNode role = authorizedGet(config, adminRealmBase(config) + "/roles-by-id/{roleId}", roleId)
                        .retrieve().body(JsonNode.class);
                if (role == null || !role.path("clientRole").asBoolean()
                        || !clientId.equals(role.path("containerId").asText())) {
                    throw new IllegalArgumentException(
                            "Keycloak role does not belong to the configured application client");
                }
                add.add(role);
            }
        }
        if (!remove.isEmpty()) {
            authorizedDelete(config, mappingsEndpoint, groupId, clientId)
                    .contentType(MediaType.APPLICATION_JSON).body(remove).retrieve().toBodilessEntity();
        }
        if (!add.isEmpty()) {
            authorizedPost(config, mappingsEndpoint, groupId, clientId)
                    .contentType(MediaType.APPLICATION_JSON).body(add).retrieve().toBodilessEntity();
        }
    }

    public List<String> getEffectiveGroupClientRoleIds(String groupId) {
        KeycloakIntegrationConfig config = configured();
        String clientId = clientUuid(config);
        String endpoint = adminRealmBase(config) + "/groups/{groupId}/role-mappings/clients/{clientId}/composite";
        return getRoleIds(config, endpoint, groupId, clientId);
    }

    public JsonNode getAuthorizationResource(String path) {
        KeycloakIntegrationConfig config = configured();
        return authorizedGet(config, authorizationResourceServerBase(config) + path)
                .retrieve().body(JsonNode.class);
    }

    public void ensureAuthorizationServices() {
        KeycloakIntegrationConfig config = configured();
        String configurationKey = config.getIssuerUri() + "|" + config.getRealm() + "|" + config.getClientId();
        if (configurationKey.equals(authorizationServicesConfigurationKey)) {
            return;
        }
        enableAuthorizationServices(config, clientUuid(config));
        authorizationServicesConfigurationKey = configurationKey;
    }

    public JsonNode getAuthorizationResource(String path, Map<String, String> queryParams) {
        KeycloakIntegrationConfig config = configured();
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(authorizationResourceServerBase(config) + path);
        queryParams.forEach(builder::queryParam);
        return authorizedGet(config, builder.build().encode().toUriString())
                .retrieve().body(JsonNode.class);
    }

    public JsonNode postAuthorizationResource(String path, Object body) {
        KeycloakIntegrationConfig config = configured();
        return authorizedPost(config, authorizationResourceServerBase(config) + path)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    public String createAuthorizationResource(String path, Object body) {
        KeycloakIntegrationConfig config = configured();
        var response = authorizedPost(config, authorizationResourceServerBase(config) + path)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
        String id = locationId(response.getHeaders());
        if (id != null) {
            return id;
        }
        JsonNode responseBody = response.getBody() == null ? null : objectMapper.valueToTree(response.getBody());
        return responseBody == null ? null : responseBody.path("_id").asText(null);
    }

    public void updateAuthorizationResource(String path, Object body) {
        KeycloakIntegrationConfig config = configured();
        authorizedPut(config, authorizationResourceServerBase(config) + path)
                .contentType(MediaType.APPLICATION_JSON).body(body).retrieve().toBodilessEntity();
    }

    public void deleteAuthorizationResource(String path) {
        KeycloakIntegrationConfig config = configured();
        authorizedDelete(config, authorizationResourceServerBase(config) + path)
                .retrieve().toBodilessEntity();
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

    /**
     * Directly assigned application client roles of a user (excluding roles
     * inherited through composites or groups).
     */
    public List<String> getDirectUserClientRoleIds(String userId) {
        KeycloakIntegrationConfig config = configured();
        String clientUuid = clientUuid(config);
        String endpoint = adminRealmBase(config) + "/users/{userId}/role-mappings/clients/{clientUuid}";
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

    private Optional<JsonNode> findClientRole(KeycloakIntegrationConfig config, String clientId, String name) {
        try {
            JsonNode role = authorizedGet(config,
                    adminRealmBase(config) + "/clients/{clientId}/roles/{roleName}", clientId, name)
                    .retrieve().body(JsonNode.class);
            return Optional.ofNullable(role);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw new IllegalStateException("Unable to read Keycloak client role", exception);
        }
    }

    private JsonNode readClientRoleById(KeycloakIntegrationConfig config, String clientId, String roleId) {
        JsonNode role = authorizedGet(config, adminRealmBase(config) + "/roles-by-id/{roleId}", roleId)
                .retrieve().body(JsonNode.class);
        if (role == null || !role.path("clientRole").asBoolean()
                || !clientId.equals(role.path("containerId").asText())) {
            throw new IllegalArgumentException("Keycloak role does not belong to the configured application client");
        }
        return role;
    }

    private JsonNode createClientRole(KeycloakIntegrationConfig config, String clientId,
            KeycloakRoleProfile profile) {
        ObjectNode representation = objectMapper.createObjectNode().put("name", profile.name());
        if (profile.description() != null) {
            representation.put("description", profile.description());
        }
        authorizedPost(config, adminRealmBase(config) + "/clients/{clientId}/roles", clientId)
                .contentType(MediaType.APPLICATION_JSON).body(representation).retrieve().toBodilessEntity();
        return findClientRole(config, clientId, profile.name())
                .orElseThrow(() -> new IllegalStateException("Keycloak client role was not created"));
    }

    private void enableAuthorizationServices(KeycloakIntegrationConfig config, String clientId) {
        String endpoint = adminRealmBase(config) + "/clients/{clientId}";
        JsonNode client = authorizedGet(config, endpoint, clientId).retrieve().body(JsonNode.class);
        if (!(client instanceof ObjectNode representation)) {
            throw new IllegalStateException("Configured Keycloak application client was not found");
        }
        if (!representation.path("authorizationServicesEnabled").asBoolean()) {
            representation.put("authorizationServicesEnabled", true);
            authorizedPut(config, endpoint, clientId)
                    .contentType(MediaType.APPLICATION_JSON).body(representation).retrieve().toBodilessEntity();
        }
    }

    private String authorizationResourceServerBase(KeycloakIntegrationConfig config) {
        return adminRealmBase(config) + "/clients/" + clientUuid(config) + "/authz/resource-server";
    }

    private void appendGroupTree(JsonNode groups, String parentId, List<KeycloakGroupProfile> result) {
        for (JsonNode group : groups) {
            String groupId = group.path("id").asText();
            result.add(new KeycloakGroupProfile(
                    groupId,
                    group.path("name").asText(),
                    attributeValue(group.path("attributes").path("code")),
                    parentId));
            JsonNode children = group.path("subGroups");
            if (children.isArray() && !children.isEmpty()) {
                appendGroupTree(children, groupId, result);
            }
        }
    }

    private String attributeValue(JsonNode value) {
        if (value.isArray()) {
            return value.isEmpty() ? null : value.get(0).asText(null);
        }
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private KeycloakRoleProfile clientRoleProfile(JsonNode role) {
        return new KeycloakRoleProfile(
                role.path("id").asText(),
                role.path("name").asText(),
                role.path("description").asText(null));
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
        String configurationKey = config.getIssuerUri() + "|" + config.getRealm() + "|" + config.getClientId();
        CachedClientUuid current = cachedClientUuid;
        if (current != null && current.configurationKey().equals(configurationKey)) {
            return current.uuid();
        }
        String uuid = lookupClientUuid(config);
        cachedClientUuid = new CachedClientUuid(configurationKey, uuid);
        return uuid;
    }

    private String lookupClientUuid(KeycloakIntegrationConfig config) {
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

    private record CachedClientUuid(String configurationKey, String uuid) {
    }
}

package io.github.benxincaomu.oa.base.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Idempotent facade over the Keycloak Authorization Services Admin REST API.
 *
 * <p>
 * OA menus and protected API URL patterns are stored as Keycloak resources,
 * bound to application client-role policies, and exposed through resource
 * permissions. Every operation looks up the object by its stable
 * application-owned name first, so retries and repeated workbench publications
 * never duplicate resources.
 * </p>
 *
 * <p>
 * Keycloak raw payloads stay inside this service; callers use
 * {@link OaStoredResource} and stable keys only.
 * </p>
 */
@Service
public class KeycloakAuthorizationService {
    private static final int PAGE_SIZE = 100;

    private static final String RESOURCE_PATH = "/resource";
    private static final String POLICY_PATH = "/policy";
    private static final String ROLE_POLICY_PATH = "/policy/role";
    private static final String PERMISSION_PATH = "/permission";
    private static final String RESOURCE_PERMISSION_PATH = "/permission/resource";

    private static final String LOGIC_POSITIVE = "POSITIVE";
    private static final String DECISION_UNANIMOUS = "UNANIMOUS";

    private final KeycloakAdminClient adminClient;
    private final ObjectMapper objectMapper;

    public KeycloakAuthorizationService(KeycloakAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = adminClient;
        this.objectMapper = objectMapper;
    }

    public void ensureAuthorizationServicesEnabled() {
        adminClient.ensureAuthorizationServices();
    }

    // ---------------------------------------------------------------- menus

    /**
     * Creates or updates a menu directory node (a node without a route).
     *
     * @return the stable resource key
     */
    public String ensureMenuGroup(Long tenantId, String groupKey, String displayName, String parentResourceKey,
            Integer sortOrder) {
        Map<String, String> attributes = menuAttributes(tenantId, null, parentResourceKey, sortOrder);
        return upsertResource(OaAuthorizationKeys.menuGroup(tenantId, groupKey), OaAuthorizationKeys.MENU_TYPE,
                displayName, null, attributes);
    }

    /**
     * Creates or updates a menu page (a node with a frontend route).
     *
     * @return the stable resource key
     */
    public String ensureMenuPage(Long tenantId, String route, String displayName, String parentResourceKey,
            Integer sortOrder) {
        Map<String, String> attributes = menuAttributes(tenantId, route, parentResourceKey, sortOrder);
        return upsertResource(OaAuthorizationKeys.menuPage(tenantId, route), OaAuthorizationKeys.MENU_TYPE,
                displayName, route, attributes);
    }

    public Optional<OaStoredResource> findMenuGroup(Long tenantId, String groupKey) {
        return findResourceByName(OaAuthorizationKeys.menuGroup(tenantId, groupKey)).map(this::toStoredResource);
    }

    public List<OaStoredResource> listMenuResources() {
        return listResourcesByType(OaAuthorizationKeys.MENU_TYPE);
    }

    // ------------------------------------------------------------------ API

    public String ensureApiResource(Long tenantId, String pattern, String displayName, String workbenchKey) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("tenantId", String.valueOf(tenantId));
        attributes.put("pattern", pattern);
        if (StringUtils.hasText(workbenchKey)) {
            attributes.put("workbench", workbenchKey);
        }
        return upsertResource(OaAuthorizationKeys.api(tenantId, pattern), OaAuthorizationKeys.API_TYPE,
                displayName, pattern, attributes);
    }

    public List<OaStoredResource> listApiResources() {
        return listResourcesByType(OaAuthorizationKeys.API_TYPE);
    }

    // ------------------------------------------------------------ role grants

    /**
     * Grants a set of resources to one application client role. The role policy
     * and the resource lookups are resolved once for the whole batch.
     */
    public void grantToRole(String roleName, Collection<String> resourceKeys) {
        if (resourceKeys == null || resourceKeys.isEmpty()) {
            return;
        }
        String policyId = ensureRolePolicy(roleName);
        for (String resourceKey : new LinkedHashSet<>(resourceKeys)) {
            ensureResourcePermission(resourceKey, roleName, policyId);
        }
    }

    public void grantToRole(String roleName, String resourceKey) {
        grantToRole(roleName, List.of(resourceKey));
    }

    public void revokeFromRole(String roleName, String resourceKey) {
        findPermissionByName(OaAuthorizationKeys.resourcePermission(resourceKey, roleName))
                .map(this::itemId)
                .ifPresent(this::deletePermission);
    }

    /**
     * Replaces every resource grant of one application client role. The
     * administrator role is protected: its baseline and default grants are
     * managed by initialization and workbench publication only.
     */
    public void replaceRoleGrants(String roleName, Collection<String> resourceKeys) {
        if (KeycloakAdminClient.OA_ADMIN_CLIENT_ROLE.equals(roleName)) {
            throw new IllegalArgumentException("The administrator role grants cannot be replaced");
        }
        String suffix = "#" + roleName;
        for (JsonNode permission : listItems(PERMISSION_PATH, Map.of())) {
            String name = permission.path("name").asText("");
            if (name.startsWith(OaAuthorizationKeys.PERMISSION_PREFIX) && name.endsWith(suffix)) {
                deletePermission(itemId(permission));
            }
        }
        grantToRole(roleName, resourceKeys);
    }

    public List<String> grantedResourceKeys(String roleName) {
        return grantedResourceKeys(List.of(roleName)).stream()
                .sorted()
                .toList();
    }

    /**
     * Returns the stable keys of all resources granted to any of the given
     * roles. Used to build the menu of an authenticated principal.
     */
    public Set<String> grantedResourceKeys(Collection<String> roleNames) {
        Set<String> granted = new LinkedHashSet<>();
        if (roleNames == null || roleNames.isEmpty()) {
            return granted;
        }
        Set<String> suffixes = new LinkedHashSet<>();
        for (String roleName : roleNames) {
            if (StringUtils.hasText(roleName)) {
                suffixes.add("#" + roleName);
            }
        }
        for (JsonNode permission : listItems(PERMISSION_PATH, Map.of())) {
            String name = permission.path("name").asText("");
            for (String suffix : suffixes) {
                if (name.startsWith(OaAuthorizationKeys.PERMISSION_PREFIX) && name.endsWith(suffix)) {
                    String resourceKey = name.substring(OaAuthorizationKeys.PERMISSION_PREFIX.length(),
                            name.length() - suffix.length());
                    granted.add(resourceKey);
                    break;
                }
            }
        }
        return granted;
    }

    // ------------------------------------------------------------- workbench

    /**
     * Removes every menu and API resource registered for one workbench,
     * including the permissions that reference them.
     */
    public void deleteWorkbenchResources(Long tenantId, String workbenchKey) {
        Set<String> keys = new LinkedHashSet<>();
        String groupKey = OaAuthorizationKeys.menuGroup(tenantId, "wb:" + workbenchKey);
        for (OaStoredResource resource : listMenuResources()) {
            if (groupKey.equals(resource.key()) || groupKey.equals(resource.parentKey())) {
                keys.add(resource.key());
            }
        }
        for (OaStoredResource resource : listApiResources()) {
            if (workbenchKey.equals(resource.workbenchKey())) {
                keys.add(resource.key());
            }
        }
        for (String key : keys) {
            deletePermissionsForResource(key);
            findResourceByName(key).map(this::itemId).ifPresent(this::deleteResource);
        }
    }

    // ------------------------------------------------------------ resources

    private String upsertResource(String key, String type, String displayName, String uri,
            Map<String, String> attributes) {
        adminClient.ensureAuthorizationServices();
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", key);
        body.put("displayName", displayName == null ? "" : displayName);
        body.put("type", type);
        ArrayNode uris = body.putArray("uris");
        if (StringUtils.hasText(uri)) {
            uris.add(uri);
        }
        ObjectNode attributeNode = body.putObject("attributes");
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            if (StringUtils.hasText(attribute.getValue())) {
                attributeNode.putArray(attribute.getKey()).add(attribute.getValue());
            }
        }

        Optional<JsonNode> existing = findResourceByName(key);
        if (existing.isPresent()) {
            String id = itemId(existing.get());
            if (!StringUtils.hasText(id)) {
                throw new IllegalStateException("Keycloak authorization resource has no ID: " + key);
            }
            adminClient.updateAuthorizationResource(RESOURCE_PATH + "/" + id, body);
            return key;
        }
        String createdId = adminClient.createAuthorizationResource(RESOURCE_PATH, body);
        if (!StringUtils.hasText(createdId)) {
            createdId = findResourceByName(key).map(this::itemId).orElse(null);
        }
        if (!StringUtils.hasText(createdId)) {
            throw new IllegalStateException("Keycloak did not return an ID for authorization resource " + key);
        }
        return key;
    }

    private List<OaStoredResource> listResourcesByType(String type) {
        List<OaStoredResource> resources = new ArrayList<>();
        for (JsonNode node : listItems(RESOURCE_PATH, Map.of("type", type))) {
            if (type.equals(node.path("type").asText())) {
                resources.add(toStoredResource(node));
            }
        }
        return resources;
    }

    private Optional<JsonNode> findResourceByName(String name) {
        return findItemByName(RESOURCE_PATH, name);
    }

    private OaStoredResource toStoredResource(JsonNode node) {
        Map<String, String> attributes = new LinkedHashMap<>();
        JsonNode attributeNode = node.path("attributes");
        if (attributeNode.isObject()) {
            attributeNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                String text = value.isArray() && !value.isEmpty() ? value.get(0).asText(null)
                        : value.isTextual() ? value.asText() : null;
                if (text != null) {
                    attributes.put(entry.getKey(), text);
                }
            });
        }
        String value = attributes.get("value");
        String parentKey = attributes.get("parent");
        Integer sortOrder = null;
        String sort = attributes.get("sortOrder");
        if (StringUtils.hasText(sort)) {
            try {
                sortOrder = Integer.valueOf(sort);
            } catch (NumberFormatException ignored) {
                // keep null; the resource is still usable
            }
        }
        String type = node.path("type").asText();
        String displayName = node.path("displayName").asText("");
        if (!StringUtils.hasText(displayName)) {
            displayName = node.path("name").asText("");
        }
        return new OaStoredResource(
                itemId(node),
                node.path("name").asText(),
                displayName,
                type,
                value,
                parentKey,
                sortOrder,
                attributes.get("workbench"),
                Map.copyOf(attributes));
    }

    private Map<String, String> menuAttributes(Long tenantId, String route, String parentResourceKey,
            Integer sortOrder) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("tenantId", String.valueOf(tenantId));
        if (StringUtils.hasText(route)) {
            attributes.put("value", route);
        }
        if (StringUtils.hasText(parentResourceKey)) {
            attributes.put("parent", parentResourceKey);
        }
        if (sortOrder != null) {
            attributes.put("sortOrder", String.valueOf(sortOrder));
        }
        return attributes;
    }

    // --------------------------------------------------- policies/permissions

    private String ensureRolePolicy(String roleName) {
        if (!StringUtils.hasText(roleName)) {
            throw new IllegalArgumentException("Application role name cannot be blank");
        }
        String policyName = OaAuthorizationKeys.rolePolicy(roleName);
        Optional<JsonNode> existing = findPolicyByName(policyName);
        if (existing.isPresent()) {
            return itemId(existing.get());
        }
        String roleId = findClientRoleId(roleName);
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", policyName);
        body.put("type", "role");
        body.put("logic", LOGIC_POSITIVE);
        body.put("decisionStrategy", DECISION_UNANIMOUS);
        ArrayNode roles = body.putArray("roles");
        roles.addObject().put("id", roleId).put("required", false);
        String createdId = adminClient.createAuthorizationResource(ROLE_POLICY_PATH, body);
        if (!StringUtils.hasText(createdId)) {
            createdId = findPolicyByName(policyName).map(this::itemId).orElse(null);
        }
        if (!StringUtils.hasText(createdId)) {
            throw new IllegalStateException("Keycloak did not return an ID for role policy " + policyName);
        }
        return createdId;
    }

    private String findClientRoleId(String roleName) {
        return adminClient.listClientRoles().stream()
                .filter(role -> roleName.equals(role.name()))
                .map(KeycloakRoleProfile::id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Keycloak client role does not exist in the OA application client: " + roleName));
    }

    private void ensureResourcePermission(String resourceKey, String roleName, String policyId) {
        String permissionName = OaAuthorizationKeys.resourcePermission(resourceKey, roleName);
        if (findPermissionByName(permissionName).isPresent()) {
            return;
        }
        String resourceId = findResourceByName(resourceKey)
                .map(this::itemId)
                .orElseThrow(() -> new IllegalStateException(
                        "Keycloak authorization resource does not exist: " + resourceKey));
        ObjectNode body = objectMapper.createObjectNode();
        body.put("name", permissionName);
        body.put("type", "resource");
        body.put("logic", LOGIC_POSITIVE);
        body.put("decisionStrategy", DECISION_UNANIMOUS);
        body.putArray("resources").add(resourceId);
        body.putArray("policies").add(policyId);
        String createdId = adminClient.createAuthorizationResource(RESOURCE_PERMISSION_PATH, body);
        if (!StringUtils.hasText(createdId) && findPermissionByName(permissionName).isEmpty()) {
            throw new IllegalStateException("Keycloak did not create resource permission " + permissionName);
        }
    }

    private Optional<JsonNode> findPolicyByName(String name) {
        return findItemByName(POLICY_PATH, name);
    }

    private Optional<JsonNode> findPermissionByName(String name) {
        return findItemByName(PERMISSION_PATH, name);
    }

    private Optional<JsonNode> findItemByName(String path, String name) {
        for (JsonNode node : listItems(path, Map.of("name", name))) {
            if (name.equals(node.path("name").asText())) {
                return Optional.of(node);
            }
        }
        return Optional.empty();
    }

    private void deletePermissionsForResource(String resourceKey) {
        String prefix = OaAuthorizationKeys.PERMISSION_PREFIX + resourceKey + "#";
        for (JsonNode permission : listItems(PERMISSION_PATH, Map.of())) {
            String name = permission.path("name").asText("");
            if (name.startsWith(prefix)) {
                deletePermission(itemId(permission));
            }
        }
    }

    private void deletePermission(String id) {
        if (StringUtils.hasText(id)) {
            adminClient.deleteAuthorizationResource(PERMISSION_PATH + "/" + id);
        }
    }

    private void deleteResource(String id) {
        if (StringUtils.hasText(id)) {
            adminClient.deleteAuthorizationResource(RESOURCE_PATH + "/" + id);
        }
    }

    private List<JsonNode> listItems(String path, Map<String, String> filters) {
        List<JsonNode> items = new ArrayList<>();
        int first = 0;
        while (true) {
            Map<String, String> query = new LinkedHashMap<>(filters);
            query.put("first", String.valueOf(first));
            query.put("max", String.valueOf(PAGE_SIZE));
            JsonNode page = adminClient.getAuthorizationResource(path, query);
            if (page == null || !page.isArray() || page.isEmpty()) {
                return items;
            }
            page.forEach(items::add);
            if (page.size() < PAGE_SIZE) {
                return items;
            }
            first += PAGE_SIZE;
        }
    }

    private String itemId(JsonNode node) {
        if (node == null) {
            return null;
        }
        String id = node.path("id").asText(null);
        return StringUtils.hasText(id) ? id : node.path("_id").asText(null);
    }
}

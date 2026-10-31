package io.github.benxincaomu.oa.base.security;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.benxincaomu.oa.base.init.OaTenantProvider;
import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;

/**
 * Read/write facade over the Keycloak directory.
 *
 * <p>
 * Reads come from the Keycloak Admin API with a short-lived in-memory cache to
 * avoid one full directory fetch per request; every write invalidates the cache.
 * No local directory row is read as a data source. Numeric OA IDs are resolved
 * only where legacy workflow data needs them, through
 * {@link OaNumericIdService}.
 * </p>
 *
 * <p>
 * Tenant scope: OA runs as a single-tenant application. Every Keycloak group
 * belongs to the default local tenant; groups are created and organised by the
 * Keycloak administrator and no {@code tenant_id} attribute is required.
 * </p>
 */
@Service
public class KeycloakDirectoryService {
    private static final long CACHE_TTL_SECONDS = 20;

    private final KeycloakAdminClient adminClient;
    private final OaNumericIdService numericIdService;
    private final OaTenantProvider tenantProvider;

    private volatile CachedGroups cachedGroups;
    private volatile CachedUsers cachedUsers;
    private volatile CachedMembers cachedMembers;

    public KeycloakDirectoryService(KeycloakAdminClient adminClient,
            OaNumericIdService numericIdService, OaTenantProvider tenantProvider) {
        this.adminClient = adminClient;
        this.numericIdService = numericIdService;
        this.tenantProvider = tenantProvider;
    }

    // ------------------------------------------------------------- tenant scope

    public Long resolveTenantIdForUser(String externalUserId) {
        if (!StringUtils.hasText(externalUserId)) {
            return null;
        }
        Map<String, Long> tenantByPath = tenantByPath(groups());
        for (OaKeycloakGroup group : adminClient.listUserGroups(externalUserId)) {
            Long tenantId = tenantIdOfPath(group.path(), tenantByPath);
            if (tenantId != null) {
                return tenantId;
            }
        }
        // 单租户模式：即使 Keycloak 用户没有加入任何组，也归属默认租户
        return tenantProvider.getTenantId();
    }

    public Optional<OaKeycloakGroup> findTenantRootGroup(Long tenantId) {
        List<OaKeycloakGroup> roots = groups().stream()
                .filter(group -> parentPath(group.path()) == null)
                .toList();
        Optional<OaKeycloakGroup> marked = roots.stream()
                .filter(group -> tenantId.equals(group.tenantId()))
                .min(Comparator.comparingInt(group -> group.path().split("/").length));
        if (marked.isPresent()) {
            return marked;
        }
        // 没有 tenant_id 标记时：只有一个顶级组就用它，多个顶级组则不强行指定根组
        return roots.size() == 1 ? Optional.of(roots.get(0)) : Optional.empty();
    }

    public List<OaKeycloakGroup> listTenantGroups(Long tenantId) {
        List<OaKeycloakGroup> allGroups = groups();
        Map<String, Long> tenantByPath = tenantByPath(allGroups);
        List<OaKeycloakGroup> tenantGroups = new ArrayList<>();
        for (OaKeycloakGroup group : allGroups) {
            if (tenantId.equals(tenantIdOfPath(group.path(), tenantByPath))) {
                tenantGroups.add(group);
            }
        }
        return tenantGroups;
    }

    // -------------------------------------------------------------------- users

    public List<KeycloakUserProfile> listTenantUsers(Long tenantId, String search) {
        Set<String> memberIds = new LinkedHashSet<>();
        Map<String, List<String>> members = memberIdsByGroup();
        for (OaKeycloakGroup group : listTenantGroups(tenantId)) {
            memberIds.addAll(members.getOrDefault(group.id(), List.of()));
        }
        Map<String, KeycloakUserProfile> users = usersById();
        String query = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        return memberIds.stream()
                .map(users::get)
                .filter(Objects::nonNull)
                .filter(profile -> query.isEmpty() || contains(profile.username(), query)
                        || contains(profile.name(), query) || contains(profile.email(), query))
                .toList();
    }

    public List<UserIdNameVo> listTenantUsersForSelection(Long tenantId, String nameLike) {
        List<KeycloakUserProfile> users = listTenantUsers(tenantId, null);
        Map<String, String> firstGroupNames = firstGroupNames(tenantId);
        String query = nameLike == null ? "" : nameLike.trim().toLowerCase(Locale.ROOT);
        List<UserIdNameVo> result = new ArrayList<>();
        for (KeycloakUserProfile profile : users) {
            if (!query.isEmpty() && !contains(profile.name(), query) && !contains(profile.username(), query)) {
                continue;
            }
            User user = numericIdService.resolveUser(profile, tenantId);
            String groupName = firstGroupNames.get(profile.id());
            String name = StringUtils.hasText(profile.name()) ? profile.name() : user.getName();
            String displayName = groupName == null ? name : name + "-" + groupName;
            result.add(new UserIdNameVo(user.getId(), displayName));
        }
        return result;
    }

    private Map<String, String> firstGroupNames(Long tenantId) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, List<String>> members = memberIdsByGroup();
        for (OaKeycloakGroup group : listTenantGroups(tenantId)) {
            for (String memberId : members.getOrDefault(group.id(), List.of())) {
                result.putIfAbsent(memberId, group.name());
            }
        }
        return result;
    }

    // ------------------------------------------------------------- departments

    public List<DeptVo> listTenantDepartments(Long tenantId) {
        List<OaKeycloakGroup> groups = listTenantGroups(tenantId);
        Map<String, String> pathToGroupId = new HashMap<>();
        Map<String, Long> groupToLocalId = new HashMap<>();
        List<DeptVo> departments = new ArrayList<>();
        for (OaKeycloakGroup group : groups) {
            String parentPath = parentPath(group.path());
            String parentGroupId = parentPath == null ? null : pathToGroupId.get(parentPath);
            Long parentLocalId = parentGroupId == null ? null : groupToLocalId.get(parentGroupId);
            Department department = numericIdService.resolveDepartment(group, tenantId, parentLocalId);
            pathToGroupId.put(group.path(), group.id());
            groupToLocalId.put(group.id(), department.getId());
            departments.add(new DeptVo(department.getId(), department.getName(), department.getParentId(),
                    department.getCode()));
        }
        return departments;
    }

    public List<DeptVo> listTenantDepartmentTree(Long tenantId) {
        List<DeptVo> flat = listTenantDepartments(tenantId);
        Map<Long, DeptVo> byId = new LinkedHashMap<>();
        for (DeptVo department : flat) {
            byId.put(department.getId(), department);
        }
        List<DeptVo> roots = new ArrayList<>();
        for (DeptVo department : flat) {
            DeptVo parent = department.getParentId() == null ? null : byId.get(department.getParentId());
            if (parent == null) {
                roots.add(department);
            } else {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(department);
            }
        }
        return roots;
    }

    public List<UserIdNameVo> listDepartmentUsers(Long tenantId, Long departmentLocalId) {
        String groupId = numericIdService.findDepartmentExternalId(departmentLocalId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "OA department has no linked Keycloak group: " + departmentLocalId));
        requireTenantGroup(groupId, tenantId);
        Map<String, KeycloakUserProfile> users = usersById();
        List<UserIdNameVo> result = new ArrayList<>();
        for (String memberId : memberIdsByGroup().getOrDefault(groupId, List.of())) {
            KeycloakUserProfile profile = users.get(memberId);
            if (profile == null) {
                continue;
            }
            User user = numericIdService.resolveUser(profile, tenantId);
            String name = StringUtils.hasText(profile.name()) ? profile.name() : user.getName();
            result.add(new UserIdNameVo(user.getId(), name));
        }
        return result;
    }

    public List<KeycloakGroupProfile> listTenantGroupProfiles(Long tenantId) {
        List<KeycloakGroupProfile> profiles = new ArrayList<>();
        for (OaKeycloakGroup group : listTenantGroups(tenantId)) {
            profiles.add(new KeycloakGroupProfile(group.id(), group.name(), group.code(), group.parentId()));
        }
        return profiles;
    }

    public List<String> listUserClientRoleIds(String externalUserId, Long tenantId) {
        requireTenantUser(externalUserId, tenantId);
        return adminClient.getDirectUserClientRoleIds(externalUserId);
    }

    public List<KeycloakGroupProfile> listUserGroupProfiles(String externalUserId, Long tenantId) {
        requireTenantUser(externalUserId, tenantId);
        List<OaKeycloakGroup> allGroups = groups();
        Map<String, Long> tenantByPath = tenantByPath(allGroups);
        List<KeycloakGroupProfile> profiles = new ArrayList<>();
        for (OaKeycloakGroup group : adminClient.listUserGroups(externalUserId)) {
            if (!tenantId.equals(tenantIdOfPath(group.path(), tenantByPath))) {
                continue;
            }
            profiles.add(new KeycloakGroupProfile(group.id(), group.name(), group.code(),
                    parentGroupId(group, allGroups)));
        }
        return profiles;
    }

    private String parentGroupId(OaKeycloakGroup group, List<OaKeycloakGroup> allGroups) {
        String parentPath = parentPath(group.path());
        if (parentPath == null) {
            return null;
        }
        return allGroups.stream()
                .filter(candidate -> candidate.path().equals(parentPath))
                .map(OaKeycloakGroup::id)
                .findFirst()
                .orElse(null);
    }

    // -------------------------------------------------------------------- roles

    public List<KeycloakRoleProfile> listClientRoles(Long tenantId) {
        return adminClient.listClientRoles();
    }

    // ------------------------------------------------- workflow identity bridge

    public Optional<KeycloakUserProfile> findUser(String externalUserId) {
        return adminClient.findUserProfile(externalUserId);
    }

    public String displayNameForUser(Long numericUserId) {
        if (numericUserId == null) {
            return null;
        }
        return numericIdService.findUserExternalId(numericUserId)
                .flatMap(adminClient::findUserProfile)
                .map(profile -> StringUtils.hasText(profile.name()) ? profile.name() : profile.username())
                .orElse(null);
    }

    /**
     * Numeric OA department IDs of the Keycloak groups a user belongs to, in the
     * order Keycloak returns them. Used by workflow candidate-group queries.
     */
    public List<Long> findDepartmentNumericIdsForUser(Long numericUserId, Long tenantId) {
        if (numericUserId == null || tenantId == null) {
            return List.of();
        }
        String externalUserId = numericIdService.findUserExternalId(numericUserId).orElse(null);
        if (externalUserId == null) {
            return List.of();
        }
        List<OaKeycloakGroup> allGroups = groups();
        Map<String, Long> tenantByPath = tenantByPath(allGroups);
        List<Long> departmentIds = new ArrayList<>();
        for (OaKeycloakGroup userGroup : adminClient.listUserGroups(externalUserId)) {
            if (!tenantId.equals(tenantIdOfPath(userGroup.path(), tenantByPath))) {
                continue;
            }
            Long departmentId = resolveGroupAncestry(userGroup, allGroups, tenantByPath, tenantId);
            if (departmentId != null && !departmentIds.contains(departmentId)) {
                departmentIds.add(departmentId);
            }
        }
        return departmentIds;
    }

    /**
     * Numeric OA ID of the leader of the user's Keycloak group, read from the
     * {@code leader_user_id} group attribute.
     */
    public Optional<Long> findLeaderNumericIdForUser(Long numericUserId, Long tenantId) {
        if (numericUserId == null || tenantId == null) {
            return Optional.empty();
        }
        String externalUserId = numericIdService.findUserExternalId(numericUserId).orElse(null);
        if (externalUserId == null) {
            return Optional.empty();
        }
        List<OaKeycloakGroup> allGroups = groups();
        Map<String, Long> tenantByPath = tenantByPath(allGroups);
        Map<String, OaKeycloakGroup> groupById = indexGroupsById(allGroups);
        for (OaKeycloakGroup userGroup : adminClient.listUserGroups(externalUserId)) {
            OaKeycloakGroup fullGroup = groupById.get(userGroup.id());
            if (fullGroup == null || !tenantId.equals(tenantIdOfPath(fullGroup.path(), tenantByPath))) {
                continue;
            }
            if (fullGroup.leaderUserId() != null) {
                return Optional.of(fullGroup.leaderUserId());
            }
        }
        return Optional.empty();
    }

    private Long resolveGroupAncestry(OaKeycloakGroup userGroup, List<OaKeycloakGroup> allGroups,
            Map<String, Long> tenantByPath, Long tenantId) {
        List<OaKeycloakGroup> chain = new ArrayList<>();
        for (OaKeycloakGroup candidate : allGroups) {
            if (!tenantId.equals(tenantIdOfPath(candidate.path(), tenantByPath))) {
                continue;
            }
            if (candidate.path().equals(userGroup.path())
                    || userGroup.path().startsWith(candidate.path() + "/")) {
                chain.add(candidate);
            }
        }
        chain.sort(Comparator.comparingInt(group -> group.path().split("/").length));
        Long parentLocalId = null;
        Long resolved = null;
        for (OaKeycloakGroup group : chain) {
            Department department = numericIdService.resolveDepartment(group, tenantId, parentLocalId);
            parentLocalId = department.getId();
            resolved = department.getId();
        }
        return resolved;
    }

    // ------------------------------------------------------------ write paths

    public KeycloakUserProfile createOrUpdateUser(KeycloakUserProfile requested, String password, boolean temporary,
            Long tenantId) {
        if (!StringUtils.hasText(requested.username())) {
            throw new IllegalArgumentException("username cannot be blank");
        }
        String existingId = adminClient.findUserIdByUsername(requested.username()).orElse(null);
        boolean creating = !StringUtils.hasText(requested.id()) && existingId == null;
        if (StringUtils.hasText(requested.id()) && existingId != null && !requested.id().equals(existingId)) {
            throw new IllegalArgumentException("Keycloak username already belongs to another identity");
        }
        String userId = creating ? null
                : StringUtils.hasText(requested.id()) ? requested.id() : existingId;
        boolean needsTenantAssignment = false;
        if (!creating) {
            Long actualTenantId = resolveTenantIdForUser(userId);
            if (actualTenantId != null && !actualTenantId.equals(tenantId)) {
                throw new IllegalArgumentException("Keycloak user is not part of the current OA tenant");
            }
            needsTenantAssignment = actualTenantId == null;
        }
        if (creating && !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("A password is required when creating a user");
        }
        KeycloakUserProfile saved = adminClient.upsertUser(requested, existingId);
        if (creating || needsTenantAssignment) {
            findTenantRootGroup(tenantId)
                    .ifPresent(rootGroup -> adminClient.setGroupMembership(saved.id(), rootGroup.id(), true));
        }
        if (StringUtils.hasText(password)) {
            adminClient.resetUserPassword(saved.id(), password, temporary);
        }
        invalidateCaches();
        return saved;
    }

    public void setUserEnabled(String userId, boolean enabled, Long tenantId) {
        requireTenantUser(userId, tenantId);
        adminClient.setUserEnabled(userId, enabled);
        invalidateCaches();
    }

    public void resetUserPassword(String userId, String password, boolean temporary, Long tenantId) {
        requireTenantUser(userId, tenantId);
        adminClient.resetUserPassword(userId, password, temporary);
    }

    public void replaceUserClientRoles(String userId, List<String> roleIds, Long tenantId) {
        requireTenantUser(userId, tenantId);
        adminClient.replaceUserClientRoles(userId, roleIds);
    }

    /**
     * Creates or updates one Keycloak group. When a group with the same name under
     * the same parent already exists in Keycloak it is adopted (its ID is reused)
     * instead of failing with a "changed after tenant validation" error.
     */
    public KeycloakGroupProfile upsertGroup(KeycloakGroupProfile requested, Long tenantId) {
        if (!StringUtils.hasText(requested.name())) {
            throw new IllegalArgumentException("group name cannot be blank");
        }
        String parentId = StringUtils.hasText(requested.parentId())
                ? requested.parentId()
                : findTenantRootGroup(tenantId).map(OaKeycloakGroup::id).orElse(null);
        if (StringUtils.hasText(parentId)) {
            requireTenantGroup(parentId, tenantId);
        }
        String groupId = requested.id();
        if (StringUtils.hasText(groupId)) {
            requireTenantGroup(groupId, tenantId);
        } else {
            groupId = adminClient.findGroupId(parentId, requested.name()).orElse(null);
        }
        KeycloakGroupProfile saved = adminClient.upsertGroup(new KeycloakGroupProfile(groupId, requested.name(),
                requested.code(), parentId));
        invalidateCaches();
        return saved;
    }

    public void setGroupMembership(String groupId, String userId, boolean member, Long tenantId) {
        requireTenantGroup(groupId, tenantId);
        requireTenantUser(userId, tenantId);
        adminClient.setGroupMembership(userId, groupId, member);
        invalidateCaches();
    }

    /**
     * Marks one Keycloak directory user as the leader of a tenant group. The OA
     * numeric user ID is stored on the group so workflow candidate resolution and
     * "starter's department leader" assignment keep working.
     */
    public void setGroupLeader(String groupId, String externalUserId, Long tenantId) {
        requireTenantGroup(groupId, tenantId);
        requireTenantUser(externalUserId, tenantId);
        KeycloakUserProfile profile = adminClient.findUserProfile(externalUserId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Keycloak user does not exist: " + externalUserId));
        User leader = numericIdService.resolveUser(profile, tenantId);
        adminClient.setGroupAttribute(groupId, "leader_user_id", String.valueOf(leader.getId()));
        invalidateCaches();
    }

    public KeycloakRoleProfile upsertClientRole(KeycloakRoleProfile requested, Long tenantId) {
        return adminClient.upsertClientRole(requested);
    }

    public void replaceGroupClientRoles(String groupId, List<String> roleIds, Long tenantId) {
        requireTenantGroup(groupId, tenantId);
        adminClient.replaceGroupClientRoles(groupId, roleIds);
    }

    // ---------------------------------------------------------------- caches

    private List<OaKeycloakGroup> groups() {
        CachedGroups current = cachedGroups;
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.groups();
        }
        List<OaKeycloakGroup> groups = adminClient.listGroupsWithAttributes();
        cachedGroups = new CachedGroups(groups, Instant.now().plusSeconds(CACHE_TTL_SECONDS));
        return groups;
    }

    private Map<String, KeycloakUserProfile> usersById() {
        CachedUsers current = cachedUsers;
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.users();
        }
        Map<String, KeycloakUserProfile> users = indexUsers(adminClient.listUsers(null));
        cachedUsers = new CachedUsers(users, Instant.now().plusSeconds(CACHE_TTL_SECONDS));
        return users;
    }

    private Map<String, List<String>> memberIdsByGroup() {
        CachedMembers current = cachedMembers;
        if (current != null && current.expiresAt().isAfter(Instant.now())) {
            return current.members();
        }
        Map<String, List<String>> members = new LinkedHashMap<>();
        for (OaKeycloakGroup group : groups()) {
            members.put(group.id(), adminClient.getGroupMemberIds(group.id()));
        }
        cachedMembers = new CachedMembers(members, Instant.now().plusSeconds(CACHE_TTL_SECONDS));
        return members;
    }

    private void invalidateCaches() {
        cachedGroups = null;
        cachedUsers = null;
        cachedMembers = null;
    }

    // --------------------------------------------------------------- utilities

    private void requireTenantUser(String externalUserId, Long tenantId) {
        Long actualTenantId = resolveTenantIdForUser(externalUserId);
        if (actualTenantId == null || !actualTenantId.equals(tenantId)) {
            throw new IllegalArgumentException("Keycloak user is not part of the current OA tenant");
        }
    }

    private void requireTenantGroup(String groupId, Long tenantId) {
        List<OaKeycloakGroup> allGroups = groups();
        Map<String, Long> tenantByPath = tenantByPath(allGroups);
        OaKeycloakGroup group = allGroups.stream()
                .filter(candidate -> candidate.id().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Keycloak group does not exist: " + groupId));
        if (!tenantId.equals(tenantIdOfPath(group.path(), tenantByPath))) {
            throw new IllegalArgumentException("Keycloak group is not part of the current OA tenant");
        }
    }

    /**
     * Pre-computes the owning tenant for every group path in one pass, so tenant
     * filtering no longer compares every group against every other group.
     */
    private Map<String, Long> tenantByPath(List<OaKeycloakGroup> allGroups) {
        Map<String, OaKeycloakGroup> byPath = new HashMap<>();
        for (OaKeycloakGroup group : allGroups) {
            byPath.put(group.path(), group);
        }
        Map<String, Long> tenantByPath = new HashMap<>();
        for (OaKeycloakGroup group : allGroups) {
            resolveTenant(group, byPath, tenantByPath);
        }
        return tenantByPath;
    }

    private Long resolveTenant(OaKeycloakGroup group, Map<String, OaKeycloakGroup> byPath,
            Map<String, Long> computed) {
        if (group.tenantId() != null) {
            return group.tenantId();
        }
        if (computed.containsKey(group.path())) {
            return computed.get(group.path());
        }
        String parent = parentPath(group.path());
        OaKeycloakGroup parentGroup = parent == null ? null : byPath.get(parent);
        // 没有 tenant_id 标记的组（包括所有顶级组）归属于默认租户
        Long tenantId = parentGroup == null ? tenantProvider.getTenantId()
                : resolveTenant(parentGroup, byPath, computed);
        computed.put(group.path(), tenantId);
        return tenantId;
    }

    private Long tenantIdOfPath(String path, Map<String, Long> tenantByPath) {
        String current = path;
        while (current != null) {
            if (tenantByPath.containsKey(current)) {
                return tenantByPath.get(current);
            }
            current = parentPath(current);
        }
        return null;
    }

    private String parentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash <= 0 ? null : path.substring(0, lastSlash);
    }

    private Map<String, OaKeycloakGroup> indexGroupsById(List<OaKeycloakGroup> groups) {
        Map<String, OaKeycloakGroup> byId = new HashMap<>();
        for (OaKeycloakGroup group : groups) {
            byId.put(group.id(), group);
        }
        return byId;
    }

    private Map<String, KeycloakUserProfile> indexUsers(List<KeycloakUserProfile> profiles) {
        Map<String, KeycloakUserProfile> users = new HashMap<>();
        for (KeycloakUserProfile profile : profiles) {
            users.put(profile.id(), profile);
        }
        return users;
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private record CachedGroups(List<OaKeycloakGroup> groups, Instant expiresAt) {
    }

    private record CachedUsers(Map<String, KeycloakUserProfile> users, Instant expiresAt) {
    }

    private record CachedMembers(Map<String, List<String>> members, Instant expiresAt) {
    }
}

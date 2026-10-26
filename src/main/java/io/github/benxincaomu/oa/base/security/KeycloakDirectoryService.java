package io.github.benxincaomu.oa.base.security;

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

import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;

/**
 * Read/write facade over the Keycloak directory.
 *
 * <p>
 * Reads always come from the Keycloak Admin API: there is no directory cache and
 * no local projection write on a read path. Numeric OA IDs are resolved only
 * where legacy workflow data needs them, through {@link OaNumericIdService}.
 * </p>
 *
 * <p>
 * Tenant scope is derived from Keycloak groups: the tenant root group carries a
 * {@code tenant_id} attribute and all of its descendants belong to that tenant.
 * Users are in scope when they are a member of at least one tenant group.
 * </p>
 */
@Service
public class KeycloakDirectoryService {
    private final KeycloakAdminClient adminClient;
    private final OaNumericIdService numericIdService;

    public KeycloakDirectoryService(KeycloakAdminClient adminClient,
            OaNumericIdService numericIdService) {
        this.adminClient = adminClient;
        this.numericIdService = numericIdService;
    }

    // ------------------------------------------------------------- tenant scope

    public Long resolveTenantIdForUser(String externalUserId) {
        if (!StringUtils.hasText(externalUserId)) {
            return null;
        }
        List<OaKeycloakGroup> allGroups = adminClient.listGroupsWithAttributes();
        for (OaKeycloakGroup group : adminClient.listUserGroups(externalUserId)) {
            Long tenantId = tenantIdOf(group, allGroups);
            if (tenantId != null) {
                return tenantId;
            }
        }
        return null;
    }

    public Optional<OaKeycloakGroup> findTenantRootGroup(Long tenantId) {
        return adminClient.listGroupsWithAttributes().stream()
                .filter(group -> tenantId.equals(group.tenantId()))
                .min(Comparator.comparingInt(group -> group.path().split("/").length));
    }

    public List<OaKeycloakGroup> listTenantGroups(Long tenantId) {
        List<OaKeycloakGroup> allGroups = adminClient.listGroupsWithAttributes();
        List<OaKeycloakGroup> tenantGroups = new ArrayList<>();
        for (OaKeycloakGroup group : allGroups) {
            if (tenantId.equals(tenantIdOf(group, allGroups))) {
                tenantGroups.add(group);
            }
        }
        return tenantGroups;
    }

    // -------------------------------------------------------------------- users

    public List<KeycloakUserProfile> listTenantUsers(Long tenantId, String search) {
        Set<String> memberIds = new LinkedHashSet<>();
        for (OaKeycloakGroup group : listTenantGroups(tenantId)) {
            memberIds.addAll(adminClient.getGroupMemberIds(group.id()));
        }
        Map<String, KeycloakUserProfile> users = indexUsers(adminClient.listUsers(null));
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
        for (OaKeycloakGroup group : listTenantGroups(tenantId)) {
            for (String memberId : adminClient.getGroupMemberIds(group.id())) {
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
        Map<String, KeycloakUserProfile> users = indexUsers(adminClient.listUsers(null));
        List<UserIdNameVo> result = new ArrayList<>();
        for (String memberId : adminClient.getGroupMemberIds(groupId)) {
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
        List<OaKeycloakGroup> allGroups = adminClient.listGroupsWithAttributes();
        List<KeycloakGroupProfile> profiles = new ArrayList<>();
        for (OaKeycloakGroup group : adminClient.listUserGroups(externalUserId)) {
            if (!tenantId.equals(tenantIdOf(group, allGroups))) {
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
        List<OaKeycloakGroup> allGroups = adminClient.listGroupsWithAttributes();
        List<Long> departmentIds = new ArrayList<>();
        for (OaKeycloakGroup userGroup : adminClient.listUserGroups(externalUserId)) {
            if (!tenantId.equals(tenantIdOf(userGroup, allGroups))) {
                continue;
            }
            Long departmentId = resolveGroupAncestry(userGroup, allGroups, tenantId);
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
        List<OaKeycloakGroup> allGroups = adminClient.listGroupsWithAttributes();
        for (OaKeycloakGroup userGroup : adminClient.listUserGroups(externalUserId)) {
            OaKeycloakGroup fullGroup = allGroups.stream()
                    .filter(candidate -> candidate.id().equals(userGroup.id()))
                    .findFirst()
                    .orElse(null);
            if (fullGroup == null || !tenantId.equals(tenantIdOf(fullGroup, allGroups))) {
                continue;
            }
            if (fullGroup.leaderUserId() != null) {
                return Optional.of(fullGroup.leaderUserId());
            }
        }
        return Optional.empty();
    }

    private Long resolveGroupAncestry(OaKeycloakGroup userGroup, List<OaKeycloakGroup> allGroups, Long tenantId) {
        List<OaKeycloakGroup> chain = new ArrayList<>();
        for (OaKeycloakGroup candidate : allGroups) {
            if (!tenantId.equals(tenantIdOf(candidate, allGroups))) {
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
            OaKeycloakGroup rootGroup = findTenantRootGroup(tenantId)
                    .orElseThrow(() -> new IllegalStateException("The tenant root group is not provisioned"));
            adminClient.setGroupMembership(saved.id(), rootGroup.id(), true);
        }
        if (StringUtils.hasText(password)) {
            adminClient.resetUserPassword(saved.id(), password, temporary);
        }
        return saved;
    }

    public void setUserEnabled(String userId, boolean enabled, Long tenantId) {
        requireTenantUser(userId, tenantId);
        adminClient.setUserEnabled(userId, enabled);
    }

    public void resetUserPassword(String userId, String password, boolean temporary, Long tenantId) {
        requireTenantUser(userId, tenantId);
        adminClient.resetUserPassword(userId, password, temporary);
    }

    public void replaceUserClientRoles(String userId, List<String> roleIds, Long tenantId) {
        requireTenantUser(userId, tenantId);
        adminClient.replaceUserClientRoles(userId, roleIds);
    }

    public KeycloakGroupProfile upsertGroup(KeycloakGroupProfile requested, Long tenantId) {
        if (!StringUtils.hasText(requested.name())) {
            throw new IllegalArgumentException("group name cannot be blank");
        }
        String parentId = StringUtils.hasText(requested.parentId())
                ? requested.parentId()
                : findTenantRootGroup(tenantId)
                        .orElseThrow(() -> new IllegalStateException("The tenant root group is not provisioned")).id();
        requireTenantGroup(parentId, tenantId);
        if (StringUtils.hasText(requested.id())) {
            requireTenantGroup(requested.id(), tenantId);
        }
        return adminClient.upsertGroup(new KeycloakGroupProfile(requested.id(), requested.name(),
                requested.code(), parentId));
    }

    public void setGroupMembership(String groupId, String userId, boolean member, Long tenantId) {
        requireTenantGroup(groupId, tenantId);
        requireTenantUser(userId, tenantId);
        adminClient.setGroupMembership(userId, groupId, member);
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
    }

    public KeycloakRoleProfile upsertClientRole(KeycloakRoleProfile requested, Long tenantId) {
        return adminClient.upsertClientRole(requested);
    }

    public void replaceGroupClientRoles(String groupId, List<String> roleIds, Long tenantId) {
        requireTenantGroup(groupId, tenantId);
        adminClient.replaceGroupClientRoles(groupId, roleIds);
    }

    // --------------------------------------------------------------- utilities

    private void requireTenantUser(String externalUserId, Long tenantId) {
        Long actualTenantId = resolveTenantIdForUser(externalUserId);
        if (actualTenantId == null || !actualTenantId.equals(tenantId)) {
            throw new IllegalArgumentException("Keycloak user is not part of the current OA tenant");
        }
    }

    private void requireTenantGroup(String groupId, Long tenantId) {
        List<OaKeycloakGroup> allGroups = adminClient.listGroupsWithAttributes();
        OaKeycloakGroup group = allGroups.stream()
                .filter(candidate -> candidate.id().equals(groupId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Keycloak group does not exist: " + groupId));
        if (!tenantId.equals(tenantIdOf(group, allGroups))) {
            throw new IllegalArgumentException("Keycloak group is not part of the current OA tenant");
        }
    }

    private Long tenantIdOf(OaKeycloakGroup group, List<OaKeycloakGroup> allGroups) {
        if (group.tenantId() != null) {
            return group.tenantId();
        }
        OaKeycloakGroup owner = null;
        for (OaKeycloakGroup candidate : allGroups) {
            if (candidate.tenantId() == null || !group.path().startsWith(candidate.path() + "/")) {
                continue;
            }
            if (owner == null || candidate.path().length() > owner.path().length()) {
                owner = candidate;
            }
        }
        return owner == null ? null : owner.tenantId();
    }

    private String parentPath(String path) {
        int lastSlash = path.lastIndexOf('/');
        return lastSlash <= 0 ? null : path.substring(0, lastSlash);
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
}

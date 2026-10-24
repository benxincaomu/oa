package io.github.benxincaomu.oa.base.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentRepository;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUser;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.RoleRepository;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
import io.github.benxincaomu.oa.bussiness.user.UserRole;
import io.github.benxincaomu.oa.bussiness.user.UserRoleRepository;

@Service
public class KeycloakDirectorySyncService {
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 10;
    private static final int EMAIL_MAX_LENGTH = 50;
    private static final int MOBILE_MAX_LENGTH = 16;

    private final KeycloakAdminClient adminClient;
    private final KeycloakObjectLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final DepartmentUserRepository departmentUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public KeycloakDirectorySyncService(KeycloakAdminClient adminClient,
            KeycloakObjectLinkRepository linkRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            DepartmentUserRepository departmentUserRepository,
            RoleRepository roleRepository,
            UserRoleRepository userRoleRepository) {
        this.adminClient = adminClient;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.departmentUserRepository = departmentUserRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public User synchronizeUser(KeycloakUserProfile profile, Long tenantId) {
        requireText(profile.username(), "username");
        requireLength(profile.username(), USERNAME_MAX_LENGTH, "username");
        Long effectiveTenantId = effectiveTenantId(tenantId);

        String usernameMatchId = adminClient.findUserIdByUsername(profile.username()).orElse(null);
        if (StringUtils.hasText(profile.id()) && usernameMatchId != null
                && !profile.id().equals(usernameMatchId)) {
            throw new IllegalArgumentException("Keycloak username already belongs to another identity");
        }
        String knownExternalId = StringUtils.hasText(profile.id()) ? profile.id() : usernameMatchId;
        User user = findLinkedUser(knownExternalId, effectiveTenantId);
        if (user == null) {
            user = userRepository.findByUserNameAndTenantId(profile.username(), effectiveTenantId);
            if (user != null) {
                requireSameTenant(user, effectiveTenantId);
                ensureLocalObjectIsNotLinked(KeycloakObjectLink.USER, knownExternalId, user.getId());
            }
        }

        KeycloakUserProfile requestedProfile = knownExternalId != null && !StringUtils.hasText(profile.id())
                ? new KeycloakUserProfile(knownExternalId, profile.username(), profile.name(), profile.email(),
                        profile.mobile(), profile.enabled())
                : profile;
        KeycloakUserProfile savedIdentity = adminClient.upsertUser(requestedProfile, usernameMatchId);
        if (knownExternalId != null && !knownExternalId.equals(savedIdentity.id())) {
            throw new IllegalStateException("Keycloak user identity changed during synchronization");
        }

        if (user == null) {
            user = new User();
            user.setTenantId(effectiveTenantId);
        }

        user.setUserName(savedIdentity.username());
        user.setName(limit(savedIdentity.name(), NAME_MAX_LENGTH));
        user.setEmail(limit(savedIdentity.email(), EMAIL_MAX_LENGTH));
        user.setMobile(limit(savedIdentity.mobile(), MOBILE_MAX_LENGTH));
        user.setEnable(savedIdentity.enabled());
        user.setPassword(null);
        user.setSalt(null);
        user.setRawPassword(false);
        user = userRepository.save(user);
        saveLinkIfMissing(KeycloakObjectLink.USER, savedIdentity.id(), user.getId());
        return user;
    }

    @Transactional
    public void disableUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, false, null);
    }

    @Transactional
    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        setUserEnabled(keycloakUserId, enabled, null);
    }

    @Transactional
    public void setUserEnabled(String keycloakUserId, boolean enabled, Long tenantId) {
        Long effectiveTenantId = effectiveTenantId(tenantId);
        User user = requireLinkedUser(keycloakUserId, effectiveTenantId);
        adminClient.setUserEnabled(keycloakUserId, enabled);
        user.setEnable(enabled);
        user.setPassword(null);
        user.setSalt(null);
        user.setRawPassword(false);
        userRepository.save(user);
    }

    @Transactional
    public Department synchronizeGroup(KeycloakGroupProfile profile, Long tenantId) {
        requireText(profile.name(), "group name");
        Long effectiveTenantId = effectiveTenantId(tenantId);
        Long parentLocalId = null;
        if (StringUtils.hasText(profile.parentId())) {
            Department parent = requireLinkedDepartment(profile.parentId(), effectiveTenantId);
            parentLocalId = parent.getId();
        }

        String parentAndNameMatchId = adminClient.findGroupId(profile.parentId(), profile.name()).orElse(null);
        if (StringUtils.hasText(profile.id()) && parentAndNameMatchId != null
                && !profile.id().equals(parentAndNameMatchId)) {
            throw new IllegalArgumentException("Keycloak group path already belongs to another group");
        }
        String knownExternalId = StringUtils.hasText(profile.id()) ? profile.id() : parentAndNameMatchId;
        Department department = findLinkedDepartment(knownExternalId, effectiveTenantId);
        if (department == null) {
            List<Department> matches = departmentRepository.findByNameAndParentIdAndTenantId(
                    profile.name(), parentLocalId, effectiveTenantId);
            department = matches.isEmpty() ? new Department() : matches.get(0);
            if (department.getId() != null) {
                requireSameTenant(department, effectiveTenantId);
                ensureLocalObjectIsNotLinked(KeycloakObjectLink.DEPARTMENT, knownExternalId, department.getId());
            }
        }

        KeycloakGroupProfile requestedProfile = knownExternalId != null && !StringUtils.hasText(profile.id())
                ? new KeycloakGroupProfile(knownExternalId, profile.name(), profile.code(), profile.parentId())
                : profile;
        KeycloakGroupProfile savedIdentity = adminClient.upsertGroup(requestedProfile, parentAndNameMatchId);
        if (knownExternalId != null && !knownExternalId.equals(savedIdentity.id())) {
            throw new IllegalStateException("Keycloak group identity changed during synchronization");
        }

        if (department.getId() == null) {
            department.setTenantId(effectiveTenantId);
        }

        department.setName(savedIdentity.name());
        department.setParentId(parentLocalId);
        department.setCode(savedIdentity.code());
        department = departmentRepository.save(department);
        saveLinkIfMissing(KeycloakObjectLink.DEPARTMENT, savedIdentity.id(), department.getId());
        return department;
    }

    @Transactional
    public void synchronizeGroupMembership(String groupId, Collection<String> memberUserIds, Long tenantId) {
        Long effectiveTenantId = effectiveTenantId(tenantId);
        KeycloakObjectLink groupLink = requireLink(KeycloakObjectLink.DEPARTMENT, groupId);
        Department department = departmentRepository.findById(groupLink.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak group link points to a missing OA department"));
        requireSameTenant(department, effectiveTenantId);

        Set<Long> desiredUserIds = new LinkedHashSet<>();
        if (memberUserIds != null) {
            for (String memberId : memberUserIds) {
                KeycloakObjectLink userLink = requireLink(KeycloakObjectLink.USER, memberId);
                User user = userRepository.findById(userLink.getLocalId())
                        .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
                requireSameTenant(user, effectiveTenantId);
                desiredUserIds.add(user.getId());
            }
        }

        for (DepartmentUser current : departmentUserRepository.findAllByDepartmentId(department.getId())) {
            if (!desiredUserIds.contains(current.getUserId())) {
                departmentUserRepository.delete(current);
            } else {
                desiredUserIds.remove(current.getUserId());
            }
        }
        for (Long userId : desiredUserIds) {
            DepartmentUser membership = new DepartmentUser();
            membership.setDepartmentId(department.getId());
            membership.setUserId(userId);
            membership.setTenantId(effectiveTenantId);
            departmentUserRepository.save(membership);
        }
    }

    @Transactional
    public void synchronizeGroupMembershipFromKeycloak(String groupId, Long tenantId) {
        synchronizeGroupMembership(groupId, adminClient.getGroupMemberIds(groupId), tenantId);
    }

    @Transactional
    public void setGroupMembership(String groupId, String userId, boolean member, Long tenantId) {
        Long effectiveTenantId = effectiveTenantId(tenantId);
        requireLinkedDepartment(groupId, effectiveTenantId);
        requireLinkedUser(userId, effectiveTenantId);
        for (String currentMemberId : adminClient.getGroupMemberIds(groupId)) {
            requireLinkedUser(currentMemberId, effectiveTenantId);
        }
        adminClient.setGroupMembership(userId, groupId, member);
        synchronizeGroupMembershipFromKeycloak(groupId, effectiveTenantId);
    }

    @Transactional
    public void synchronizeUserRoleMappings(String keycloakUserId) {
        KeycloakObjectLink userLink = requireLink(KeycloakObjectLink.USER, keycloakUserId);
        User user = userRepository.findById(userLink.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
        Long tenantId = effectiveTenantId(user.getTenantId());
        requireSameTenant(user, tenantId);

        Set<Long> localRoleIds = new LinkedHashSet<>();
        List<String> externalRoleIds = adminClient.getEffectiveUserRoleIds(keycloakUserId);
        validateLinkedRoleTenants(externalRoleIds, tenantId);
        for (String externalRoleId : externalRoleIds) {
            KeycloakObjectLink roleLink = linkRepository
                    .findByObjectTypeAndExternalId(KeycloakObjectLink.ROLE, externalRoleId)
                    .orElse(null);
            if (roleLink == null) {
                continue;
            }
            Role role = roleRepository.findById(roleLink.getLocalId())
                    .orElseThrow(() -> new IllegalStateException("Keycloak role link points to a missing OA role"));
            requireSameTenant(role, tenantId);
            localRoleIds.add(role.getId());
        }

        userRoleRepository.deleteByUserId(user.getId());
        for (Long roleId : localRoleIds) {
            UserRole userRole = new UserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(roleId);
            userRole.setTenantId(tenantId);
            userRoleRepository.save(userRole);
        }
    }

    @Transactional
    public void replaceUserClientRoles(String keycloakUserId, Collection<String> roleIds) {
        Long tenantId = effectiveTenantId(null);
        requireLinkedUser(keycloakUserId, tenantId);
        validateLinkedRoleTenants(adminClient.getEffectiveUserRoleIds(keycloakUserId), tenantId);
        validateLinkedRoleTenants(roleIds, tenantId);
        adminClient.replaceUserClientRoles(keycloakUserId, roleIds);
        synchronizeUserRoleMappings(keycloakUserId);
    }

    public Long resolveLocalUserId(String keycloakSubject) {
        return requireLink(KeycloakObjectLink.USER, keycloakSubject).getLocalId();
    }

    private KeycloakObjectLink requireLink(String objectType, String externalId) {
        requireText(externalId, "Keycloak external ID");
        return linkRepository.findByObjectTypeAndExternalId(objectType, externalId)
                .orElseThrow(() -> new IllegalStateException("Keycloak object has not been linked to an OA projection"));
    }

    private User requireLinkedUser(String externalId, Long tenantId) {
        KeycloakObjectLink link = requireLink(KeycloakObjectLink.USER, externalId);
        User user = userRepository.findById(link.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
        requireSameTenant(user, tenantId);
        return user;
    }

    private User findLinkedUser(String externalId, Long tenantId) {
        if (!StringUtils.hasText(externalId)) {
            return null;
        }
        KeycloakObjectLink link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.USER, externalId)
                .orElse(null);
        if (link == null) {
            return null;
        }
        User user = userRepository.findById(link.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
        requireSameTenant(user, tenantId);
        return user;
    }

    private Department requireLinkedDepartment(String externalId, Long tenantId) {
        KeycloakObjectLink link = requireLink(KeycloakObjectLink.DEPARTMENT, externalId);
        Department department = departmentRepository.findById(link.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak group link points to a missing OA department"));
        requireSameTenant(department, tenantId);
        return department;
    }

    private Department findLinkedDepartment(String externalId, Long tenantId) {
        if (!StringUtils.hasText(externalId)) {
            return null;
        }
        KeycloakObjectLink link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.DEPARTMENT, externalId)
                .orElse(null);
        if (link == null) {
            return null;
        }
        Department department = departmentRepository.findById(link.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak group link points to a missing OA department"));
        requireSameTenant(department, tenantId);
        return department;
    }

    private void validateLinkedRoleTenants(Collection<String> roleIds, Long tenantId) {
        if (roleIds == null) {
            return;
        }
        for (String externalRoleId : roleIds) {
            requireText(externalRoleId, "Keycloak role ID");
            KeycloakObjectLink roleLink = linkRepository
                    .findByObjectTypeAndExternalId(KeycloakObjectLink.ROLE, externalRoleId)
                    .orElse(null);
            if (roleLink != null) {
                Role role = roleRepository.findById(roleLink.getLocalId())
                        .orElseThrow(() -> new IllegalStateException("Keycloak role link points to a missing OA role"));
                requireSameTenant(role, tenantId);
            }
        }
    }

    private void saveLinkIfMissing(String objectType, String externalId, Long localId) {
        KeycloakObjectLink byExternal = linkRepository.findByObjectTypeAndExternalId(objectType, externalId).orElse(null);
        if (byExternal != null) {
            if (!byExternal.getLocalId().equals(localId)) {
                throw new IllegalStateException("Keycloak external ID is already linked to another OA object");
            }
            return;
        }
        ensureLocalObjectIsNotLinked(objectType, externalId, localId);
        KeycloakObjectLink link = new KeycloakObjectLink();
        link.setObjectType(objectType);
        link.setExternalId(externalId);
        link.setLocalId(localId);
        linkRepository.save(link);
    }

    private void ensureLocalObjectIsNotLinked(String objectType, String externalId, Long localId) {
        linkRepository.findByObjectTypeAndLocalId(objectType, localId).ifPresent(existing -> {
            if (!existing.getExternalId().equals(externalId)) {
                throw new IllegalStateException("OA object is already linked to another Keycloak object");
            }
        });
    }

    private Long effectiveTenantId(Long tenantId) {
        Long effective = tenantId == null ? JpaAuditorAware.getCurrentTenantId() : tenantId;
        if (effective == null) {
            throw new IllegalStateException("An OA tenant context is required to synchronize directory data");
        }
        return effective;
    }

    private void requireSameTenant(io.github.benxincaomu.oa.base.entity.BaseEntity entity, Long tenantId) {
        if (entity.getTenantId() == null || !entity.getTenantId().equals(tenantId)) {
            throw new IllegalArgumentException("Keycloak directory object belongs to another OA tenant");
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
    }

    private void requireLength(String value, int maxLength, String field) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds the OA projection length limit of " + maxLength);
        }
    }
}

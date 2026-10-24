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

        KeycloakUserProfile savedIdentity = adminClient.upsertUser(profile);
        KeycloakObjectLink link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.USER, savedIdentity.id())
                .orElse(null);

        User user;
        if (link != null) {
            user = userRepository.findById(link.getLocalId())
                    .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
            requireSameTenant(user, effectiveTenantId);
        } else {
            user = userRepository.findByUserNameAndTenantId(savedIdentity.username(), effectiveTenantId);
            if (user == null) {
                user = new User();
                user.setTenantId(effectiveTenantId);
            } else {
                requireSameTenant(user, effectiveTenantId);
                ensureLocalObjectIsNotLinked(KeycloakObjectLink.USER, savedIdentity.id(), user.getId());
            }
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
        setUserEnabled(keycloakUserId, false);
    }

    @Transactional
    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        KeycloakObjectLink link = requireLink(KeycloakObjectLink.USER, keycloakUserId);
        User user = userRepository.findById(link.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
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
            parentLocalId = requireLink(KeycloakObjectLink.DEPARTMENT, profile.parentId()).getLocalId();
        }

        KeycloakGroupProfile savedIdentity = adminClient.upsertGroup(profile);
        KeycloakObjectLink link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.DEPARTMENT, savedIdentity.id())
                .orElse(null);
        Department department;
        if (link != null) {
            department = departmentRepository.findById(link.getLocalId())
                    .orElseThrow(() -> new IllegalStateException("Keycloak group link points to a missing OA department"));
            requireSameTenant(department, effectiveTenantId);
        } else {
            List<Department> matches = departmentRepository.findByNameAndParentIdAndTenantId(
                    savedIdentity.name(), parentLocalId, effectiveTenantId);
            department = matches.isEmpty() ? new Department() : matches.get(0);
            if (department.getId() != null) {
                ensureLocalObjectIsNotLinked(KeycloakObjectLink.DEPARTMENT, savedIdentity.id(), department.getId());
            } else {
                department.setTenantId(effectiveTenantId);
            }
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
        adminClient.setGroupMembership(userId, groupId, member);
        synchronizeGroupMembershipFromKeycloak(groupId, tenantId);
    }

    @Transactional
    public void synchronizeUserRoleMappings(String keycloakUserId) {
        KeycloakObjectLink userLink = requireLink(KeycloakObjectLink.USER, keycloakUserId);
        User user = userRepository.findById(userLink.getLocalId())
                .orElseThrow(() -> new IllegalStateException("Keycloak user link points to a missing OA user"));
        Long tenantId = user.getTenantId();

        Set<Long> localRoleIds = new LinkedHashSet<>();
        for (String externalRoleId : adminClient.getEffectiveUserRoleIds(keycloakUserId)) {
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
        if (entity.getTenantId() != null && !entity.getTenantId().equals(tenantId)) {
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

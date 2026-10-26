package io.github.benxincaomu.oa.base.security;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.benxincaomu.oa.bussiness.organization.Department;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentRepository;
import io.github.benxincaomu.oa.bussiness.user.Role;
import io.github.benxincaomu.oa.bussiness.user.RoleRepository;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;

/**
 * Maintains the numeric-ID projections and display snapshots required by
 * workflow and audit data.
 *
 * <p>
 * Keycloak remains the source of truth: this service is only allowed to
 * allocate/reuse an OA numeric ID and refresh the snapshot row that existing
 * workflow queries join on. It is never used as a source for directory reads.
 * </p>
 */
@Service
public class OaNumericIdService {
    private static final int USERNAME_MAX_LENGTH = 20;
    private static final int NAME_MAX_LENGTH = 10;
    private static final int EMAIL_MAX_LENGTH = 50;
    private static final int MOBILE_MAX_LENGTH = 16;

    private final KeycloakObjectLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    public OaNumericIdService(KeycloakObjectLinkRepository linkRepository,
            UserRepository userRepository,
            DepartmentRepository departmentRepository,
            RoleRepository roleRepository) {
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
    }

    public User resolveUser(KeycloakUserProfile profile, Long tenantId) {
        requireText(profile.id(), "Keycloak user ID");
        requireTenant(tenantId);
        Optional<KeycloakObjectLink> link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.USER, profile.id());
        User user = link.map(found -> userRepository.findById(found.getLocalId()).orElse(null)).orElse(null);
        if (user == null) {
            user = userRepository.findByUserNameAndTenantId(profile.username(), tenantId);
            if (user == null) {
                User usernameOwner = userRepository.findByUserName(profile.username());
                if (usernameOwner != null && !tenantId.equals(usernameOwner.getTenantId())) {
                    throw new IllegalArgumentException("Keycloak username belongs to another OA tenant");
                }
                user = new User();
                user.setTenantId(tenantId);
            }
        }
        if (user.getTenantId() == null) {
            user.setTenantId(tenantId);
        }
        if (!tenantId.equals(user.getTenantId())) {
            throw new IllegalArgumentException("Keycloak user belongs to another OA tenant");
        }
        String userName = limit(profile.username(), USERNAME_MAX_LENGTH);
        String name = limit(profile.name(), NAME_MAX_LENGTH);
        String email = limit(profile.email(), EMAIL_MAX_LENGTH);
        String mobile = limit(profile.mobile(), MOBILE_MAX_LENGTH);
        boolean changed = user.getId() == null
                || !Objects.equals(user.getUserName(), userName)
                || !Objects.equals(user.getName(), name)
                || !Objects.equals(user.getEmail(), email)
                || !Objects.equals(user.getMobile(), mobile)
                || !Objects.equals(user.getEnable(), profile.enabled())
                || user.getPassword() != null
                || user.getSalt() != null
                || !Boolean.FALSE.equals(user.getRawPassword());
        user.setUserName(userName);
        user.setName(name);
        user.setEmail(email);
        user.setMobile(mobile);
        user.setEnable(profile.enabled());
        user.setPassword(null);
        user.setSalt(null);
        user.setRawPassword(false);
        User saved = changed ? userRepository.save(user) : user;
        saveLinkIfMissing(KeycloakObjectLink.USER, profile.id(), saved.getId());
        return saved;
    }

    public Department resolveDepartment(OaKeycloakGroup group, Long tenantId, Long parentLocalId) {
        requireText(group.id(), "Keycloak group ID");
        requireTenant(tenantId);
        Optional<KeycloakObjectLink> link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.DEPARTMENT, group.id());
        Department department = link.map(found -> departmentRepository.findById(found.getLocalId()).orElse(null))
                .orElse(null);
        if (department == null) {
            List<Department> matches = departmentRepository.findByNameAndParentIdAndTenantId(group.name(),
                    parentLocalId, tenantId);
            department = matches.isEmpty() ? new Department() : matches.get(0);
        }
        if (department.getTenantId() == null) {
            department.setTenantId(tenantId);
        }
        if (!tenantId.equals(department.getTenantId())) {
            throw new IllegalArgumentException("Keycloak group belongs to another OA tenant");
        }
        Long leaderUserId = group.leaderUserId() != null ? group.leaderUserId() : department.getLeaderUserId();
        boolean changed = department.getId() == null
                || !Objects.equals(department.getName(), group.name())
                || !Objects.equals(department.getParentId(), parentLocalId)
                || !Objects.equals(department.getCode(), group.code())
                || !Objects.equals(department.getLeaderUserId(), leaderUserId);
        department.setName(group.name());
        department.setParentId(parentLocalId);
        department.setCode(group.code());
        department.setLeaderUserId(leaderUserId);
        Department saved = changed ? departmentRepository.save(department) : department;
        saveLinkIfMissing(KeycloakObjectLink.DEPARTMENT, group.id(), saved.getId());
        return saved;
    }

    public Role resolveRole(KeycloakRoleProfile profile, Long tenantId) {
        requireText(profile.id(), "Keycloak role ID");
        requireTenant(tenantId);
        Optional<KeycloakObjectLink> link = linkRepository
                .findByObjectTypeAndExternalId(KeycloakObjectLink.ROLE, profile.id());
        Role role = link.map(found -> roleRepository.findById(found.getLocalId()).orElse(null)).orElse(null);
        if (role == null) {
            role = roleRepository.findByNameAndTenantId(profile.name(), tenantId);
            if (role == null) {
                role = new Role();
            }
        }
        if (role.getTenantId() == null) {
            role.setTenantId(tenantId);
        }
        if (!tenantId.equals(role.getTenantId())) {
            throw new IllegalArgumentException("Keycloak role belongs to another OA tenant");
        }
        role.setName(profile.name());
        Role saved = roleRepository.save(role);
        saveLinkIfMissing(KeycloakObjectLink.ROLE, profile.id(), saved.getId());
        return saved;
    }

    public Optional<Long> findUserId(String externalId) {
        return linkRepository.findByObjectTypeAndExternalId(KeycloakObjectLink.USER, externalId)
                .map(KeycloakObjectLink::getLocalId);
    }

    public Optional<String> findUserExternalId(Long localId) {
        return linkRepository.findByObjectTypeAndLocalId(KeycloakObjectLink.USER, localId)
                .map(KeycloakObjectLink::getExternalId);
    }

    public Optional<Long> findDepartmentId(String externalId) {
        return linkRepository.findByObjectTypeAndExternalId(KeycloakObjectLink.DEPARTMENT, externalId)
                .map(KeycloakObjectLink::getLocalId);
    }

    public Optional<String> findDepartmentExternalId(Long localId) {
        return linkRepository.findByObjectTypeAndLocalId(KeycloakObjectLink.DEPARTMENT, localId)
                .map(KeycloakObjectLink::getExternalId);
    }

    private void saveLinkIfMissing(String objectType, String externalId, Long localId) {
        Optional<KeycloakObjectLink> byExternal = linkRepository
                .findByObjectTypeAndExternalId(objectType, externalId);
        if (byExternal.isPresent()) {
            if (!byExternal.get().getLocalId().equals(localId)) {
                throw new IllegalStateException("Keycloak external ID is already linked to another OA object");
            }
            return;
        }
        linkRepository.findByObjectTypeAndLocalId(objectType, localId).ifPresent(existing -> {
            if (!existing.getExternalId().equals(externalId)) {
                throw new IllegalStateException("OA object is already linked to another Keycloak object");
            }
        });
        KeycloakObjectLink link = new KeycloakObjectLink();
        link.setObjectType(objectType);
        link.setExternalId(externalId);
        link.setLocalId(localId);
        linkRepository.save(link);
    }

    private void requireTenant(Long tenantId) {
        if (tenantId == null) {
            throw new IllegalStateException("An OA tenant context is required to map Keycloak identities");
        }
    }

    private void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}

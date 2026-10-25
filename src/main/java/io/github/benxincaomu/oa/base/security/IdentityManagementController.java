package io.github.benxincaomu.oa.base.security;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.init.OaBaselineAuthorizationProvisioner;
import jakarta.validation.Valid;

/**
 * Keycloak-backed identity management API.
 *
 * <p>
 * Every read comes from Keycloak; every write goes to Keycloak. OA local
 * directory rows are not read or written here.
 * </p>
 */
@RestController
@RequestMapping("/identity")
public class IdentityManagementController {
    private final KeycloakDirectoryService directoryService;
    private final KeycloakAuthorizationService authorizationService;
    private final OaBaselineAuthorizationProvisioner baselineAuthorizationProvisioner;

    public IdentityManagementController(KeycloakDirectoryService directoryService,
            KeycloakAuthorizationService authorizationService,
            OaBaselineAuthorizationProvisioner baselineAuthorizationProvisioner) {
        this.directoryService = directoryService;
        this.authorizationService = authorizationService;
        this.baselineAuthorizationProvisioner = baselineAuthorizationProvisioner;
    }

    @GetMapping("/users")
    public List<KeycloakUserProfile> listUsers(@RequestParam(required = false) String search) {
        return directoryService.listTenantUsers(tenantId(), search);
    }

    @GetMapping("/users/{userId}/roles")
    public List<String> listUserRoles(@PathVariable String userId) {
        return directoryService.listUserClientRoleIds(userId, tenantId());
    }

    @GetMapping("/users/{userId}/groups")
    public List<KeycloakGroupProfile> listUserGroups(@PathVariable String userId) {
        return directoryService.listUserGroupProfiles(userId, tenantId());
    }

    @PutMapping("/users")
    public KeycloakUserProfile upsertUser(@Valid @RequestBody KeycloakUserCommand command) {
        if (command == null || command.user() == null) {
            throw new IllegalArgumentException("User profile is required");
        }
        return directoryService.createOrUpdateUser(command.user(), command.password(),
                command.temporaryPassword(), tenantId());
    }

    @PutMapping("/users/{userId}/enabled")
    public void setUserEnabled(@PathVariable String userId, @RequestBody EnabledCommand command) {
        directoryService.setUserEnabled(userId, command.enabled(), tenantId());
    }

    @PutMapping("/users/{userId}/password")
    public void resetUserPassword(@PathVariable String userId, @RequestBody PasswordCommand command) {
        directoryService.resetUserPassword(userId, command.password(), command.temporary(), tenantId());
    }

    @PutMapping("/users/{userId}/roles")
    public void replaceUserRoles(@PathVariable String userId,
            @RequestBody KeycloakRoleAssignmentCommand command) {
        directoryService.replaceUserClientRoles(userId, command.roleIds(), tenantId());
    }

    @GetMapping("/groups")
    public List<KeycloakGroupProfile> listGroups() {
        return directoryService.listTenantGroupProfiles(tenantId());
    }

    @PutMapping("/groups")
    public KeycloakGroupProfile upsertGroup(@RequestBody KeycloakGroupProfile group) {
        return directoryService.upsertGroup(group, tenantId());
    }

    @PutMapping("/groups/{groupId}/members/{userId}")
    public void setGroupMembership(@PathVariable String groupId, @PathVariable String userId,
            @RequestParam boolean member) {
        directoryService.setGroupMembership(groupId, userId, member, tenantId());
    }

    @PutMapping("/groups/{groupId}/leader")
    public void setGroupLeader(@PathVariable String groupId, @RequestBody GroupLeaderCommand command) {
        directoryService.setGroupLeader(groupId, command.userId(), tenantId());
    }

    @GetMapping("/roles")
    public List<KeycloakRoleProfile> listRoles() {
        return directoryService.listClientRoles(tenantId());
    }

    @GetMapping("/resources")
    public List<OaStoredResource> listResources() {
        List<OaStoredResource> resources = new java.util.ArrayList<>();
        resources.addAll(authorizationService.listMenuResources());
        resources.addAll(authorizationService.listApiResources());
        return resources;
    }

    /**
     * 幂等预置基线菜单/接口资源与 oa-admin 默认授权。
     * 全新初始化会自动执行；存量部署升级后可手动调用一次。
     */
    @PutMapping("/authorization-baseline")
    public void provisionBaseline() {
        baselineAuthorizationProvisioner.provision(tenantId());
    }

    @GetMapping("/roles/{roleName}/resources")
    public List<String> listRoleResources(@PathVariable String roleName) {
        return authorizationService.grantedResourceKeys(roleName);
    }

    @PutMapping("/roles/{roleName}/resources")
    public void replaceRoleResources(@PathVariable String roleName,
            @RequestBody RoleResourceCommand command) {
        authorizationService.replaceRoleGrants(roleName, command.resourceKeys());
    }

    @PutMapping("/roles")
    public KeycloakRoleProfile upsertRole(@RequestBody KeycloakRoleProfile role) {
        return directoryService.upsertClientRole(role, tenantId());
    }

    @PutMapping("/groups/{groupId}/roles")
    public void replaceGroupRoles(@PathVariable String groupId,
            @RequestBody KeycloakRoleAssignmentCommand command) {
        directoryService.replaceGroupClientRoles(groupId, command.roleIds(), tenantId());
    }

    private Long tenantId() {
        Long tenantId = JpaAuditorAware.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("An OA tenant context is required for identity management");
        }
        return tenantId;
    }
}

record KeycloakUserCommand(KeycloakUserProfile user, String password, boolean temporaryPassword) {
}

record EnabledCommand(boolean enabled) {
}

record PasswordCommand(String password, boolean temporary) {
}

record KeycloakRoleAssignmentCommand(List<String> roleIds) {
}

record GroupLeaderCommand(String userId) {
}

record RoleResourceCommand(List<String> resourceKeys) {
}

package io.github.benxincaomu.oa.base.security;

/**
 * Keycloak group projection used by the OA directory adapter.
 *
 * @param id           Keycloak group ID
 * @param name         group name (OA department name)
 * @param path         full Keycloak group path
 * @param parentId     Keycloak parent group ID, {@code null} for top-level groups
 * @param code         OA department code stored as the {@code code} attribute
 * @param tenantId     OA tenant ID stored on a tenant root group
 * @param leaderUserId OA numeric user ID stored as the {@code leader_user_id}
 *                     attribute
 */
public record OaKeycloakGroup(
        String id,
        String name,
        String path,
        String parentId,
        String code,
        Long tenantId,
        Long leaderUserId) {
}

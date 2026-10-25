package io.github.benxincaomu.oa.base.security;

import java.util.List;
import java.util.Map;

/**
 * Read model for a Keycloak authorization resource owned by OA.
 *
 * @param id           Keycloak resource ID
 * @param key          stable application-owned resource name
 * @param displayName  user-visible name
 * @param type         {@link OaAuthorizationKeys#MENU_TYPE} or
 *                     {@link OaAuthorizationKeys#API_TYPE}
 * @param value        menu route (pages only) or API URL pattern
 * @param parentKey    parent menu resource key, {@code null} for top-level menus
 * @param sortOrder    menu ordering hint
 * @param workbenchKey workbench key for dynamically published resources
 * @param attributes   remaining Keycloak resource attributes (first value per key)
 */
public record OaStoredResource(
        String id,
        String key,
        String displayName,
        String type,
        String value,
        String parentKey,
        Integer sortOrder,
        String workbenchKey,
        Map<String, String> attributes) {

    public boolean isMenu() {
        return OaAuthorizationKeys.MENU_TYPE.equals(type);
    }

    public boolean isMenuGroup() {
        return isMenu() && value == null;
    }
}

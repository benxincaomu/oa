package io.github.benxincaomu.oa.base.security;

import java.util.Locale;

/**
 * Stable, application-owned names for Keycloak Authorization Services objects.
 *
 * <p>
 * Names never include generated Keycloak IDs so that retries can look up and
 * update the same object. Menus are resources of type {@link #MENU_TYPE}; each
 * protected API pattern is a resource of type {@link #API_TYPE}. Every OA role
 * has one reusable client-role policy, and each (resource, role) pair has one
 * resource permission.
 * </p>
 */
public final class OaAuthorizationKeys {
    public static final String MENU_TYPE = "oa-menu";
    public static final String API_TYPE = "oa-api";

    public static final String ROLE_POLICY_PREFIX = "oa-policy-role-";
    public static final String PERMISSION_PREFIX = "oa-perm:";

    private static final String MENU_GROUP_PREFIX = "oa:%d:mengroup:";
    private static final String MENU_PAGE_PREFIX = "oa:%d:menu:";
    private static final String API_PREFIX = "oaapi:%d:";

    private OaAuthorizationKeys() {
    }

    public static String menuGroup(Long tenantId, String groupKey) {
        return String.format(Locale.ROOT, MENU_GROUP_PREFIX, tenantId) + groupKey;
    }

    public static String menuPage(Long tenantId, String route) {
        return String.format(Locale.ROOT, MENU_PAGE_PREFIX, tenantId) + route;
    }

    public static String api(Long tenantId, String pattern) {
        return String.format(Locale.ROOT, API_PREFIX, tenantId) + pattern;
    }

    public static String rolePolicy(String roleName) {
        return ROLE_POLICY_PREFIX + roleName;
    }

    public static String resourcePermission(String resourceName, String roleName) {
        return PERMISSION_PREFIX + resourceName + "#" + roleName;
    }
}

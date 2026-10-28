package io.github.benxincaomu.oa.base.init.vo;

/**
 * Current initialization state used by the init page.
 *
 * @param initialized        whether local tenant data exists
 * @param keycloakConfigured whether {@code app.keycloak.*} is fully configured
 * @param keycloakLinked     whether the administrator / root department / admin
 *                           role are linked to Keycloak objects
 */
public record InitStatusVo(boolean initialized, boolean keycloakConfigured, boolean keycloakLinked) {
}

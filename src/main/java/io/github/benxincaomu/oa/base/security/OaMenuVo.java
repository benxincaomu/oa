package io.github.benxincaomu.oa.base.security;

/**
 * Menu node returned to the frontend. The {@code id} is the stable Keycloak
 * resource name, not a local numeric ID.
 */
public record OaMenuVo(String id, String name, String value, String type, java.util.List<OaMenuVo> children) {
}

package io.github.benxincaomu.oa.base.security;

public record KeycloakUserProfile(
        String id,
        String username,
        String name,
        String email,
        String mobile,
        boolean enabled) {
}

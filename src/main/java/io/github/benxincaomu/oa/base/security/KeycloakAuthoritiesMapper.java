package io.github.benxincaomu.oa.base.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KeycloakAuthoritiesMapper implements GrantedAuthoritiesMapper {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final KeycloakConnectionService connectionService;
    private final ObjectMapper objectMapper;

    public KeycloakAuthoritiesMapper(KeycloakConnectionService connectionService, ObjectMapper objectMapper) {
        this.connectionService = connectionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new LinkedHashSet<>(authorities);
        String clientId = connectionService.getConfiguration().map(KeycloakIntegrationConfig::getClientId).orElse(null);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcAuthority) {
                Map<String, Object> claims = oidcAuthority.getAttributes();
                addRoles(mapped, claims.get("realm_access"));
                addClientRoles(mapped, claims.get("resource_access"), clientId);
                addGroups(mapped, claims.get("groups"));
                if (oidcAuthority.getUserInfo() != null) {
                    var userInfo = oidcAuthority.getUserInfo();
                    addRoles(mapped, userInfo.getClaims().get("realm_access"));
                    addClientRoles(mapped, userInfo.getClaims().get("resource_access"), clientId);
                    addGroups(mapped, userInfo.getClaims().get("groups"));
                }
            }
        }
        return mapped;
    }

    private void addRoles(Set<GrantedAuthority> authorities, Object accessValue) {
        Map<String, Object> access = asMap(accessValue);
        addNames(authorities, access.get("roles"), "ROLE_");
    }

    private void addClientRoles(Set<GrantedAuthority> authorities, Object accessValue, String clientId) {
        if (clientId == null) {
            return;
        }
        Map<String, Object> resourceAccess = asMap(accessValue);
        addRoles(authorities, resourceAccess.get(clientId));
    }

    private void addGroups(Set<GrantedAuthority> authorities, Object groupsValue) {
        if (groupsValue instanceof Iterable<?> groups) {
            for (Object group : groups) {
                if (group instanceof String name && !name.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority("GROUP_" + name.replace('/', '_')));
                }
            }
        }
    }

    private void addNames(Set<GrantedAuthority> authorities, Object namesValue, String prefix) {
        if (namesValue instanceof Iterable<?> names) {
            for (Object name : names) {
                if (name instanceof String value && !value.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(prefix + value));
                }
            }
        }
    }

    private Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, MAP_TYPE);
        }
        return Map.of();
    }
}

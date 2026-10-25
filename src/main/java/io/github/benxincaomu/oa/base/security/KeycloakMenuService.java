package io.github.benxincaomu.oa.base.security;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Builds the authenticated user's menu from the Keycloak authorization resources
 * granted to the user's application client roles.
 */
@Service
public class KeycloakMenuService {
    private static final Comparator<OaStoredResource> MENU_ORDER = Comparator
            .comparingInt((OaStoredResource menu) -> menu.sortOrder() == null ? Integer.MAX_VALUE
                    : menu.sortOrder())
            .thenComparing(menu -> menu.displayName() == null ? "" : menu.displayName());

    private final KeycloakAuthorizationService authorizationService;

    public KeycloakMenuService(KeycloakAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public List<OaMenuVo> menusForRoles(Collection<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return List.of();
        }
        Set<String> grantedKeys = authorizationService.grantedResourceKeys(roleNames);
        if (grantedKeys.isEmpty()) {
            return List.of();
        }
        Map<String, OaStoredResource> menusByKey = new LinkedHashMap<>();
        for (OaStoredResource menu : authorizationService.listMenuResources()) {
            menusByKey.put(menu.key(), menu);
        }

        Set<String> visible = new LinkedHashSet<>();
        Deque<String> pending = new ArrayDeque<>();
        for (String key : grantedKeys) {
            if (menusByKey.containsKey(key)) {
                visible.add(key);
                pending.add(key);
            }
        }
        while (!pending.isEmpty()) {
            OaStoredResource menu = menusByKey.get(pending.poll());
            if (menu == null) {
                continue;
            }
            String parentKey = menu.parentKey();
            if (StringUtils.hasText(parentKey) && visible.add(parentKey)) {
                pending.add(parentKey);
            }
        }

        Map<String, List<OaStoredResource>> childrenByParent = new LinkedHashMap<>();
        List<OaStoredResource> roots = new ArrayList<>();
        for (String key : visible) {
            OaStoredResource menu = menusByKey.get(key);
            if (menu == null) {
                continue;
            }
            String parentKey = menu.parentKey();
            if (parentKey == null || !visible.contains(parentKey)) {
                roots.add(menu);
            } else {
                childrenByParent.computeIfAbsent(parentKey, ignored -> new ArrayList<>()).add(menu);
            }
        }
        roots.sort(MENU_ORDER);
        return roots.stream().map(menu -> toVo(menu, childrenByParent)).toList();
    }

    private OaMenuVo toVo(OaStoredResource menu, Map<String, List<OaStoredResource>> childrenByParent) {
        List<OaMenuVo> children = childrenByParent.getOrDefault(menu.key(), List.of()).stream()
                .sorted(MENU_ORDER)
                .map(child -> toVo(child, childrenByParent))
                .toList();
        return new OaMenuVo(menu.key(), menu.displayName(), menu.value(), null,
                children.isEmpty() ? null : children);
    }
}

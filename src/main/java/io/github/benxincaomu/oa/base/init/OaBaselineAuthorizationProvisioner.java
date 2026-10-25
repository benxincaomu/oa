package io.github.benxincaomu.oa.base.init;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import io.github.benxincaomu.oa.base.security.KeycloakAdminClient;
import io.github.benxincaomu.oa.base.security.KeycloakAuthorizationService;

/**
 * Provisions the baseline OA menu and API resources into Keycloak and grants
 * them to the platform administrator role.
 *
 * <p>
 * Runs during first-run initialization, after the tenant exists and the
 * administrator identity is linked, and is idempotent: re-running it updates the
 * same resources instead of duplicating them.
 * </p>
 */
@Component
public class OaBaselineAuthorizationProvisioner {

    private final KeycloakAuthorizationService authorizationService;

    public OaBaselineAuthorizationProvisioner(KeycloakAuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    public void provision(Long tenantId) {
        authorizationService.ensureAuthorizationServicesEnabled();
        List<String> grantedResourceKeys = new ArrayList<>();
        int groupOrder = 0;
        for (OaBaselineAuthorizationCatalog.MenuGroup group : OaBaselineAuthorizationCatalog.MENU_GROUPS) {
            String groupKey = authorizationService.ensureMenuGroup(tenantId, group.key(), group.displayName(), null,
                    groupOrder++);
            grantedResourceKeys.add(groupKey);
            int pageOrder = 0;
            for (OaBaselineAuthorizationCatalog.MenuPage page : group.pages()) {
                grantedResourceKeys.add(authorizationService.ensureMenuPage(tenantId, page.route(),
                        page.displayName(), groupKey, pageOrder++));
            }
        }
        for (OaBaselineAuthorizationCatalog.ApiResource api : OaBaselineAuthorizationCatalog.API_RESOURCES) {
            grantedResourceKeys.add(authorizationService.ensureApiResource(tenantId, api.pattern(),
                    api.displayName(), null));
        }
        authorizationService.grantToRole(KeycloakAdminClient.OA_ADMIN_CLIENT_ROLE, grantedResourceKeys);
    }
}

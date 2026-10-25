package io.github.benxincaomu.oa.base.init;

import java.util.List;

/**
 * Baseline menu and API resources provisioned into Keycloak during first-run
 * initialization. Keys are stable; changing a display name or route is a data
 * update, not a new resource.
 *
 * <p>
 * The catalog mirrors the permissions previously seeded into the local
 * {@code permission} table. Dynamic workbench menus are not listed here; they are
 * registered by workbench publication.
 * </p>
 */
public final class OaBaselineAuthorizationCatalog {

    public record MenuGroup(String key, String displayName, List<MenuPage> pages) {
    }

    public record MenuPage(String route, String displayName) {
    }

    public record ApiResource(String pattern, String displayName) {
    }

    public static final List<MenuGroup> MENU_GROUPS = List.of(
            new MenuGroup("system", "系统管理", List.of(
                    new MenuPage("/app/permissions", "权限管理"),
                    new MenuPage("/app/usermanage", "用户管理"),
                    new MenuPage("/app/roles", "角色管理"),
                    new MenuPage("/app/organizes", "组织管理"),
                    new MenuPage("/app/workbenches", "设计工作台"))));

    public static final List<ApiResource> API_RESOURCES = List.of(
            new ApiResource("/permission/**", "权限管理接口"),
            new ApiResource("/user/**", "用户管理接口"),
            new ApiResource("/role/**", "角色管理接口"),
            new ApiResource("/organize/**", "组织管理接口"),
            new ApiResource("/identity/**", "身份管理接口"),
            new ApiResource("/workbench/**", "工作台接口"),
            new ApiResource("/entityDefinition/**", "实体设计接口"),
            new ApiResource("/entityEnum/**", "实体枚举接口"),
            new ApiResource("/workflowDefinition/**", "流程定义接口"),
            new ApiResource("/file/**", "文件接口"),
            new ApiResource("/actuator/**", "运维接口"));

    private OaBaselineAuthorizationCatalog() {
    }
}

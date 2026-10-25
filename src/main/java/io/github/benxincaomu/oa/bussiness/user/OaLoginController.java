package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.security.KeycloakMenuService;
import io.github.benxincaomu.oa.base.security.OaMenuVo;
import io.github.benxincaomu.oa.base.security.OaOidcUserPrincipal;
import io.github.benxincaomu.oa.bussiness.user.vo.SimpleUserInfo;

@RestController
@RequestMapping("login")
public class OaLoginController {
    private final KeycloakMenuService menuService;

    public OaLoginController(KeycloakMenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping("myInfo")
    public SimpleUserInfo myInfo(@AuthenticationPrincipal OaOidcUserPrincipal principal) {
        if (principal == null) {
            return null;
        }
        SimpleUserInfo info = new SimpleUserInfo();
        info.setName(principal.getDisplayName());
        info.setUserName(principal.getUserName());
        return info;
    }

    /**
     * 返回当前登录用户在 Keycloak 中被授权的菜单资源树。
     */
    @GetMapping("getMyMenus")
    public List<OaMenuVo> getMyMenus(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return List.of();
        }
        List<String> roleNames = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority != null && authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .toList();
        return menuService.menusForRoles(roleNames);
    }
}

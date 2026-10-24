package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.security.OaOidcUserPrincipal;
import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;
import io.github.benxincaomu.oa.bussiness.user.vo.SimpleUserInfo;

@RestController
@RequestMapping("login")
public class OaLoginController {
    private final RoleRepository roleRepository;
    private final PermissionService permissionService;

    public OaLoginController(RoleRepository roleRepository, PermissionService permissionService) {
        this.roleRepository = roleRepository;
        this.permissionService = permissionService;
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

    @GetMapping("getMyMenus")
    public List<PermissionIdName> getMyMenus(@AuthenticationPrincipal OaOidcUserPrincipal principal) {
        if (principal == null) {
            return List.of();
        }
        Role role = roleRepository.findByUserId(principal.getOaUserId());
        return role == null ? List.of() : permissionService.getMenuTreeByRoleId(role.getId());
    }
}

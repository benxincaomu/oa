package io.github.benxincaomu.oa.bussiness.user;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.bussiness.user.vo.AuthVo;
import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("role")
public class RoleController {

    @Resource
    private RoleService roleService;

    @Resource
    private PermissionService permissionService;

    /**
     * 角色列表
     * 
     * @param currPage 当前页
     * @param pageSize 每页大小
     * @param name     角色名
     * @return
     */
    @GetMapping("list")
    public PagedModel<Role> list(Integer currPage, Integer pageSize, String name) {
        PagedModel<Role> page = new PagedModel<>(roleService.roles(currPage, pageSize, name));
        return page;
    }

    @GetMapping("listAll")
    public List<Role> listAll() {
        return roleService.listAll();
    }

    @PostMapping
    public Role insert(@RequestBody Role role) {
        return roleService.save(role);
    }

    /**
     * 根据角色id获取权限树
     * @param roleId
     * @return
     */
    @GetMapping("permissionsByRoleId")
    List<PermissionIdName> permissionsByRoleId(Long roleId) {
        return permissionService.getMenuTreeByRoleId(roleId);
    }

    /**
     * 对角色授权
     */
    @PostMapping("auth")
    public void auth(@Validated @RequestBody AuthVo authVo){
        List<RolePermission> rolePermissions = new ArrayList<>();
        for (Long permissionId : authVo.getPermissionIds()) { 
            RolePermission rolePermission = new RolePermission();
            rolePermission.setRoleId(authVo.getRoleId());
            rolePermission.setPermissionId(permissionId);
            rolePermissions.add(rolePermission);
        }
        roleService.auth(authVo.getRoleId(), rolePermissions);
    }

}

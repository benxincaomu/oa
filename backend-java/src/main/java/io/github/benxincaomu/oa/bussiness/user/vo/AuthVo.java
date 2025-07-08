package io.github.benxincaomu.oa.bussiness.user.vo;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;

/**
 * 角色授权
 */
public class AuthVo {
    
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    
    private Long[] permissionIds;
    public Long getRoleId() {
        return roleId;
    }
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
    public Long[] getPermissionIds() {
        return permissionIds;
    }
    public void setPermissionIds(Long[] permissionIds) {
        this.permissionIds = permissionIds;
    }

    
}

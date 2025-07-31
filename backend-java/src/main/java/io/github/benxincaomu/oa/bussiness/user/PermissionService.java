package io.github.benxincaomu.oa.bussiness.user;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;
import jakarta.annotation.Resource;

@Service
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    @Resource
    private PermissionRepository permissionRepository;

    @Resource
    private RoleRepository roleRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    PermissionService(RolePermissionRepository rolePermissionRepository) {
        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<PermissionIdName> findAllMenuTree() {
        List<PermissionIdName> top = permissionRepository.findTopMenu();
        if (!CollectionUtils.isEmpty(top)) {
            for (PermissionIdName permission : top) {
                permission.setChildren(findWithParentId(permission.getId()));
            }
        }
        return top;
    }

    List<PermissionIdName> findWithParentId(Long parentId) {
        List<PermissionIdName> byParentId = permissionRepository.findByParentMenuId(parentId);
        if (!CollectionUtils.isEmpty(byParentId)) {
            for (PermissionIdName permission : byParentId) {
                permission.setChildren(findWithParentId(permission.getId()));
            }
        }
        return byParentId;
    }

    public Permission insert( Permission permission) {
        if(permission.getParentId() != null){
            permissionRepository.findById(permission.getParentId()).ifPresent(p -> permission.setParentName(p.getName()));
        }
        boolean exists = permission.getId()!= null;
        
        permissionRepository.save(permission);
        if(!exists){
            // 为当前租户的初始角色授权
            Long roleId = roleRepository.findMinRoleIdByTenantId(JpaAuditorAware.getCurrentTenantId()).orElse(null);
            if(roleId != null){
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRoleId(roleId);
                rolePermission.setPermissionId(permission.getId());
    
                rolePermissionRepository.save(rolePermission);
            }
        }

        return permissionRepository.save(permission);
    }

    public Page<Permission>  permissions(String name, Integer type, Integer currPage, Integer pageSize) {
        currPage = currPage == null || currPage < 0 ? 0 : currPage;
        pageSize = pageSize == null || pageSize < 1 ? 20 : currPage;

        PageRequest page = PageRequest.of(currPage, pageSize, Sort.by("createAt"));

        ExampleMatcher matcher = ExampleMatcher.matching();
        Permission permission = new Permission();
        if (name != null && !name.isBlank()) {
            permission.setName(name);
            matcher.withMatcher("name", m->m.exact());
        }
        if(type != null && type > 0){
            permission.setType(type);
            matcher.withMatcher("type", m->m.exact());
        }

        Example<Permission> example = Example.of(permission,matcher);
        Page<Permission> permissions = permissionRepository.findAll(example, page);
        return permissions;
    }


    public List<PermissionIdName>findByType(Integer type) {
        return permissionRepository.findIdAndNameByType(type);
    }

    public List<PermissionIdName> getMenuTreeByRoleId(Long roleId) {
        // List<PermissionIdName> permissions = rolePermissionRepository.findTreeByRoleId(roleId);
        List<PermissionIdName> permissions = fillPersmissionByRoleId(roleId,null);


        return permissions;
    }


    List<PermissionIdName> fillPersmissionByRoleId(Long roleId,Long parentId) { 
        List<PermissionIdName> permissions = rolePermissionRepository.findPermissionIdNameByRoleId(roleId,parentId);
        if(!CollectionUtils.isEmpty(permissions)){
            for(PermissionIdName permission:permissions){
                permission.setChildren(fillPersmissionByRoleId(roleId,permission.getId()));
            }
        }

        return permissions;
    }

    
}

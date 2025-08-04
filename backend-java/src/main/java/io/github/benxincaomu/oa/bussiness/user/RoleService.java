package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import io.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.BaseService;
import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import jakarta.annotation.Resource;

@Service
public class RoleService extends BaseService{
    @Resource
    private RoleRepository roleRepository;

    @Resource
    private RolePermissionRepository rolePermissionRepository;


    public Page<Role> roles(Integer currPage, Integer pageSize, String name) {
        currPage = currPage == null || currPage<0? 0 : currPage-1;
        pageSize = pageSize == null || pageSize<0? 20 : pageSize;
        PageRequest page = PageRequest.of(currPage, pageSize, Sort.by("createAt"));
        Role role = new Role();
        ExampleMatcher matcher = ExampleMatcher.matching();
        if(name != null && !name.isBlank()){
            role.setName(name);
            matcher.withMatcher("name", m->m.exact());
        }
        Example<Role> example = Example.of(role, matcher);
        Page<Role> list = roleRepository.findAll(example, page);
        
        return list;
    }

    public Role save(Role role) {
        return roleRepository.save(role);
    }

    /**
     * 角色授权
     * @param roleId 角色授权
     * @param rolePermissions 角色权限
     */
    @Transactional
    public void auth(Long roleId, List<RolePermission> rolePermissions) {
        roleRepository.findMinRoleIdByTenantId(JpaAuditorAware.getCurrentTenantId()).ifPresent(id->{
            Asserts.equals(roleId, id, OaResponseCode.DEFAULT_ROLE_CAN_NOT_AUTH);
        });;
        rolePermissionRepository.deleteByRoleId(roleId);
        if(!CollectionUtils.isEmpty(rolePermissions)){

            rolePermissionRepository.saveAll(rolePermissions);
            
        }
        
    }

    public List<Role> listAll() {
        Long tenantId = JpaAuditorAware.getCurrentTenantId();
        return roleRepository.findByTenantId(tenantId);
    }


}

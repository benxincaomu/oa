package io.github.benxincaomu.oa.bussiness.user;

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

import io.github.benxincaomu.oa.bussiness.user.vo.ParentPermission;
import jakarta.annotation.Resource;

@Service
public class PermissionService {

    @Resource
    private PermissionRepository permissionRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public List<Permission> findAllWithParentId() {
        List<Permission> top = permissionRepository.findTop();
        if (!CollectionUtils.isEmpty(top)) {
            for (Permission permission : top) {
                permission.setChildren(findWithParentId(permission.getId()));
            }
        }
        return null;
    }

    List<Permission> findWithParentId(Long parentId) {
        List<Permission> byParentId = permissionRepository.findByParentId(parentId);
        if (!CollectionUtils.isEmpty(byParentId)) {
            for (Permission permission : byParentId) {
                permission.setChildren(findWithParentId(permission.getId()));
            }
        }
        return byParentId;
    }

    public int insert(Permission permission) {
        permissionRepository.save(permission);
        return permissionRepository.save(permission).getId() == null ? 0 : 1;
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


    public List<ParentPermission>findByType(Integer type) {
        return permissionRepository.findIdAndNameByType(type);
    }
}

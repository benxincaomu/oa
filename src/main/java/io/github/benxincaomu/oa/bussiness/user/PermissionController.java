package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.web.PagedModel;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("permission")
public class PermissionController {
    @Resource
    private PermissionService permissionService;

     private final Logger logger = LoggerFactory.getLogger(getClass());

    // @PostMapping
    public int insert(@RequestBody Permission permission){
        return permissionService.insert(permission).getId() == null ? 0 : 1;
    }

    @GetMapping("list")
    public PagedModel<Permission> list(String name,Integer type,Integer currPage,Integer pageSize){
        logger.info("Thread is virtual {}",Thread.currentThread().isVirtual());
        PagedModel<Permission> page = new PagedModel<>(permissionService.permissions(name, type, currPage, pageSize));
        return page;
    }


    /**
     * 获取指定类型的权限
     * @return
     */
    // @GetMapping("permissionsByType")
    public List<PermissionIdName> permissionsByType(@Validated @Nonnull Integer type){

        return permissionService.findByType(type);
    }

    @GetMapping("getMenuTree")
    public List<PermissionIdName> getMenuTree(){
        return permissionService.findAllMenuTree();

    }
}

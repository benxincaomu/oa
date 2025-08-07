package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.data.web.PagedModel;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

@RestController
@RequestMapping("/entityEnum")
public class EntityEnumController {
    @Resource
    private EntityEnumService entityEnumService;

    @RequestMapping("{id}")
    public EntityEnum getById(Long id) {
        return entityEnumService.getById(id);
    }

    @RequestMapping("list")
    public PagedModel<EntityEnum> list(String name,@Validated @NonNull Long workbenchId, Integer currPage, Integer pageSize) {
        return new PagedModel<>(entityEnumService.list(name,workbenchId, currPage, pageSize));
    }
}

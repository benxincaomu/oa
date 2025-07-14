package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.List;

import javax.swing.text.html.parser.Entity;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.utils.Pair;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;




@RestController
@RequestMapping("entityDefinition")
public class EntityDefinitionController {

    @Resource
    private EntityDefinitionService entityDefinitionService;
    @RequestMapping("list")
    public PagedModel<EntityDefinition> list(String entityName,Integer currPage,Integer pageSize) {
        PagedModel<EntityDefinition> page = new PagedModel<>(entityDefinitionService.list(entityName,currPage,pageSize));
        return page;
    }

    @GetMapping("getColumnTypes")
    public Pair[] getMethodName() {
        return Consts.COLUMN_TYPES;
    }

    @GetMapping("getByWorkbenchId/{wid}")
    public EntityDefinition getByWorkbenchId(@PathVariable("wid") Long workbenchId) {
        return entityDefinitionService.getByWorkbenchId(workbenchId);
    }

    @PostMapping
    public Long insert(@RequestBody EntityDefinition entity) {
        entityDefinitionService.insert(entity);
        
        return entity.getId();
    }
    
    
    
}

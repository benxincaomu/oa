package io.github.benxincaomu.oa.bussiness.workflow;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class EntityDefinitionService {

    @Resource
    private EntityDefinitionRepository entityDefinitionRepository;

    public Page<EntityDefinition> list(String entityName, Integer currPage, Integer pageSize) {
        currPage = currPage == null ? 0 : currPage - 1;
        pageSize = pageSize == null ? 20 : pageSize;
        PageRequest page = PageRequest.of(currPage, pageSize, Sort.by(Sort.Order.desc("createAt")));
        EntityDefinition entityDefinition = new EntityDefinition();
        ExampleMatcher matcher = ExampleMatcher.matching();
        if (entityName != null && !entityName.isEmpty()) {
            entityDefinition.setEntityName(entityName);
            matcher.withMatcher("entityName", m -> m.exact());
        }
        Example<EntityDefinition> example = Example.of(entityDefinition, matcher);
        Page<EntityDefinition> entityDefinitions = entityDefinitionRepository.findAll(example, page);
        return entityDefinitions;
    }

    public EntityDefinition getEntityDefinitionById(Long id) {
        return entityDefinitionRepository.findById(id).orElse(null);
    }

    public EntityDefinition getByWorkbenchId(Long id) {
        return entityDefinitionRepository.findOneByWorkbenchId(id).orElse(null);
    }

    public EntityDefinition insert(EntityDefinition entity) {
        return entityDefinitionRepository.save(entity);
    }


}

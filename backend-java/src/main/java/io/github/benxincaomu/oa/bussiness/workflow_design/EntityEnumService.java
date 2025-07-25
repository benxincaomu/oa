package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class EntityEnumService {

    @Resource
    private EntityEnumRepository entityEnumRepository;

    public EntityEnum getById(Long id) {
        return entityEnumRepository.findById(id).orElse(null);
    }

    public Page<EntityEnum> list(String name,Long workbenchId, Integer currPage, Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null || currPage < 0 ? 0 : currPage,
                pageSize == null || pageSize < 0 ? 20 : pageSize);
        ExampleMatcher matcher = ExampleMatcher.matching();
        EntityEnum entityEnum = new EntityEnum();
        entityEnum.setWorkbenchId(workbenchId);
        matcher.withMatcher("workbenchId", m -> m.exact());
        if (name != null && !name.isEmpty()) {
            matcher.withMatcher("name", m -> m.exact());
            entityEnum.setName(name);
        }
        Example<EntityEnum> example = Example.of(entityEnum, matcher);
        return entityEnumRepository.findAll(example, page);
        
    }

}

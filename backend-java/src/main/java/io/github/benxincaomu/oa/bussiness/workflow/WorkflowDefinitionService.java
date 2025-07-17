package io.github.benxincaomu.oa.bussiness.workflow;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

@Service
public class WorkflowDefinitionService {
    @Resource
    private WorkflowDefinitionRepository workflowDefinitionRepository;

    public WorkflowDefinition getByWorkbenchId(Long workbenchId) {
        return workflowDefinitionRepository.findOneByWorkbenchId(workbenchId).orElse(null);
    }


    @Transactional
    public Long save(WorkflowDefinition workflowDefinition) {
        if (workflowDefinition.getId() == null) {
            Long id = workflowDefinitionRepository.save(workflowDefinition).getId();
            if (id != null) {
                workflowDefinition.setId(id);
            }
        }
        workflowDefinitionRepository.save(workflowDefinition);

        return workflowDefinition.getId();
    }

}

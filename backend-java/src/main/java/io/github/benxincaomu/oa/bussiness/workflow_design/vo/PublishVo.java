package io.github.benxincaomu.oa.bussiness.workflow_design.vo;

import io.github.benxincaomu.oa.bussiness.workflow_design.EntityDefinition;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkflowDefinition;

public class PublishVo {
    private Long workbenchId;
    private EntityDefinition entityDefinition;
    private WorkflowDefinition workflowDefinition;
    public Long getWorkbenchId() {
        return workbenchId;
    }
    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }
    public EntityDefinition getEntityDefinition() {
        return entityDefinition;
    }
    public void setEntityDefinition(EntityDefinition entityDefinition) {
        this.entityDefinition = entityDefinition;
    }
    public WorkflowDefinition getWorkflowDefinition() {
        return workflowDefinition;
    }
    public void setWorkflowDefinition(WorkflowDefinition workflowDefinition) {
        this.workflowDefinition = workflowDefinition;
    }

    
}

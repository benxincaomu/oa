package io.github.benxincaomu.oa.bussiness.workflow;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Table(name = "workflow_definition",indexes = {@Index(name = "workflow_definition_workbench_id_index",columnList = "workbenchId")})
@Entity
public class WorkflowDefinition extends BaseEntity {

    @Comment("流程定义")
    @Column(name = "flow_definition",columnDefinition = "text")
    private String flowDefinition;

    @Comment("工作台id")
    private Long workbenchId;

    public String getFlowDefinition() {
        return flowDefinition;
    }

    public void setFlowDefinition(String flowDefinition) {
        this.flowDefinition = flowDefinition;
    }

    public Long getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }


    
    
}

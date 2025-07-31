package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "flow_form_assignee_sample",indexes = {@Index(name = "flow_form_assignee_sample_workbench_id_index", columnList = "workbenchId,actived")})
@Comment("流程表单审批人表")
public class FlowFormAssignee extends BaseEntity {
    @Comment("流程表单ID")
    private Long flowFormId;
    @Comment("审批人")
    private Long assignee;
    @Comment("候选组")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<Long> candidateGroups;

    @Comment("工作台ID")
    private Long workbenchId;

    @Comment("是否激活")
    private Boolean actived;
    public Long getFlowFormId() {
        return flowFormId;
    }
    public void setFlowFormId(Long flowFormId) {
        this.flowFormId = flowFormId;
    }
    public Long getAssignee() {
        return assignee;
    }
    public void setAssignee(Long assignee) {
        this.assignee = assignee;
    }
    public List<Long> getCandidateGroups() {
        return candidateGroups == null? new ArrayList<>() : candidateGroups;
    }
    public void setCandidateGroups(List<Long> candidateGroups) {
        this.candidateGroups = candidateGroups;
    }
    public Long getWorkbenchId() {
        return workbenchId;
    }
    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }
    public Boolean getActived() {
        return actived;
    }
    public void setActived(Boolean actived) {
        this.actived = actived;
    }
    
}

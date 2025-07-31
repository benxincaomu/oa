package io.github.benxincaomu.oa.bussiness.workflow;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "flow_history_sample", indexes = {
        @Index(name = "flow_history_sample_flow_form_id_index", columnList = "flowFormId") })
@Comment("流程历史示例表")
public class FlowHistory extends BaseEntity {
    @Comment("流程表单id")
    private Long flowFormId;

    @Comment("备注-流向名称")
    @Column(columnDefinition = "varchar(20)")
    private String flowName;

    @Column(columnDefinition = "varchar(50)", length = 50)
    private String flowId;

    @Comment("审批意见")
    @Column(columnDefinition = "varchar(200)", length = 200)
    private String approvalOpinion;

    @Comment("操作人姓名")
    @Column(columnDefinition = "varchar(20)", length = 20)
    private String operatorName;

    public Long getFlowFormId() {
        return flowFormId;
    }

    public void setFlowFormId(Long flowFormId) {
        this.flowFormId = flowFormId;
    }

    public String getFlowName() {
        return flowName;
    }

    public void setFlowName(String flowName) {
        this.flowName = flowName;
    }

    public String getApprovalOpinion() {
        return approvalOpinion;
    }

    public void setApprovalOpinion(String approvalOpinion) {
        this.approvalOpinion = approvalOpinion;
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }


    
}

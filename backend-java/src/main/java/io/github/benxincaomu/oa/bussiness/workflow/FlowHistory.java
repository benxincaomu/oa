package io.github.benxincaomu.oa.bussiness.workflow;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "flow_history_sample",indexes = {@Index(name = "flow_history_sample_flow_form_id_index",columnList = "flowFormId")})
@Comment("流程历史示例表")
public class FlowHistory extends BaseEntity {
    @Comment("流程表单id")
    private Long flowFormId;

    @Comment("备注-流向名称")
    private String flowName;

    @Comment("审批意见")
    @Column(columnDefinition = "varchar(200)",length = 200)
    private String approvalOpinion;
}

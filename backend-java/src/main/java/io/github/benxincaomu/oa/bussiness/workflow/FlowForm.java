package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "flow_form_data_sample", indexes = {
        @Index(name = "flow_form_data_sample_create_by_index", columnList = "createBy"),
        @Index(name = "flow_form_data_sample_publish_id_index", columnList = "publishId"),
})
public class FlowForm extends BaseEntity {
    @Comment("流程发布ID")
    private Long publishId;

    @Comment("字段列表")
    @Column(name = "data", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> data;

    @Comment("流程实例ID")
    @Column(name = "process_id", columnDefinition = "varchar(36)")
    @JsonIgnore
    private String processId;

    @Comment("流程部署name")
    @Column(name = "deploy_name", columnDefinition = "varchar(36)")
    @JsonIgnore
    private String deployName;

    @Transient
    private List<FlowHistory> flowHistories;

    public boolean isStarted() {
        return processId != null;
    }

    public Long getPublishId() {
        return publishId;
    }

    public void setPublishId(Long publishId) {
        this.publishId = publishId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    public String getDeployName() {
        return deployName;
    }

    public void setDeployName(String deployName) {
        this.deployName = deployName;
    }

    public List<FlowHistory> getFlowHistories() {
        return flowHistories;
    }

    public void setFlowHistories(List<FlowHistory> flowHistories) {
        this.flowHistories = flowHistories;
    }

 

}

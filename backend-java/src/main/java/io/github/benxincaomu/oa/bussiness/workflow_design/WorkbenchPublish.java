package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "workbench_publish", indexes = {
        @Index(name = "workbench_publish_workbench_id_index", columnList = "workbenchId") })
public class WorkbenchPublish extends BaseEntity {

    @Comment("工作台id")
    private Long workbenchId;

    @Comment("实体定义")
    @Column(name = "columns", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private EntityDefinition entityDefinition;

    @Comment("工作流部署id")
    private String workflowDeployId;

    @Comment("版本号")
    private Long version;

    @Comment("对应表单存储的表名称")
    @Column(columnDefinition = "varchar(30)", length = 30)
    private String flowFormTable;

    @Comment("对应流程历史的表名称")
    @Column(columnDefinition = "varchar(30)", length = 30)
    private String flowHistoryTable;

    public Long getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public EntityDefinition getEntityDefinition() {
        return entityDefinition;
    }

    public void setEntityDefinition(EntityDefinition entityDefinition) {
        this.entityDefinition = entityDefinition;
    }

    public String getWorkflowDeployId() {
        return workflowDeployId;
    }

    public void setWorkflowDeployId(String workflowDeployId) {
        this.workflowDeployId = workflowDeployId;
    }

    public String getFlowFormTable() {
        return flowFormTable;
    }

    public void setFlowFormTable(String flowFormTable) {
        this.flowFormTable = flowFormTable;
    }

    public String getFlowHistoryTable() {
        return flowHistoryTable;
    }

    public void setFlowHistoryTable(String flowHistoryTable) {
        this.flowHistoryTable = flowHistoryTable;
    }

    
}

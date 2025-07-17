package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.List;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "entity_definition",indexes = {@Index(name = "entity_definition_workbench_id_index",columnList = "workbenchId")})
@Comment("实体定义")
public class EntityDefinition extends BaseEntity{
    @Comment("实体名称")
    @Column(name = "entity_name",columnDefinition = "varchar(20) not null")
    private String entityName;

    @Comment("实体描述")
    private String entityDesc;

    @Comment("数据所属表名")
    private String dataTable;

    @Comment("版本")
    private Integer version;

    @Comment("设计工作台id")
    private Long workbenchId;

    @Comment("字段列表")
    @Column(name = "columns",columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<EntityColumn> columns;

    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getDataTable() {
        return dataTable;
    }

    public void setDataTable(String dataTable) {
        this.dataTable = dataTable;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<EntityColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<EntityColumn> columns) {
        this.columns = columns;
    }

    public String getEntityDesc() {
        return entityDesc;
    }

    public void setEntityDesc(String entityDesc) {
        this.entityDesc = entityDesc;
    }

    public Long getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }

    
}

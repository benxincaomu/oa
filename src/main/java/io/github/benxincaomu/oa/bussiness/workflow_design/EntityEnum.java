package io.github.benxincaomu.oa.bussiness.workflow_design;

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
@Comment("实体枚举")
@Table(name = "entity_enum",indexes = {@Index(name = "entity_enum_workbench_id_index",columnList = "workbenchId")})
public class EntityEnum extends BaseEntity {
    
    @Comment("枚举ID")
    private Long workbenchId;

    @Comment("枚举名称")
    @Column(name = "name",columnDefinition = "varchar(20)")
    private String name;

    @Comment("枚举列表")
    @Column(name = "items",columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<EnumItem> items;

    public Long getWorkbenchId() {
        return workbenchId;
    }

    public void setWorkbenchId(Long workbenchId) {
        this.workbenchId = workbenchId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<EnumItem> getItems() {
        return items;
    }

    public void setItems(List<EnumItem> items) {
        this.items = items;
    }

    
}

class EnumItem { 
    private Long value;
    private String lable;
    public Long getValue() {
        return value;
    }
    public void setValue(Long value) {
        this.value = value;
    }
    public String getLable() {
        return lable;
    }
    public void setLable(String lable) {
        this.lable = lable;
    }

    
}

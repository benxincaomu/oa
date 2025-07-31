package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.hibernate.annotations.Comment;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 项目，作为entity、workflow、页面的入口和上层
 */
@Entity
@Table(name = "workbench")
@Comment("设计工作台")
public class Workbench extends BaseEntity {
    @Comment("工作台名称")
    @Column(columnDefinition = "varchar(20) ")
    private String name;
    @Comment("工作台描述")
    @Column(columnDefinition = "varchar(255) ")
    private String description;

    @Comment("工作台key,部署流程使用，与页面无关")
    @Column(columnDefinition = "varchar(36)",updatable = false)
    @JsonIgnore
    private String workbenchKey;

    @Comment("工作台版本")
    private Long version;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Long getVersion() {
        return version;
    }
    public void setVersion(Long version) {
        this.version = version;
    }
    public String getWorkbenchKey() {
        return workbenchKey;
    }
    public void setWorkbenchKey(String workbenchKey) {
        this.workbenchKey = workbenchKey;
    }

    

}

package io.github.benxincaomu.oa.bussiness.workflow;

import org.hibernate.annotations.Comment;

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

    

}

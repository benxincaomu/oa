package io.github.benxincaomu.oa.bussiness.user;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "permission")
public class Permission extends BaseEntity {
    @Comment("权限类型:1--菜单,2--请求url")
    private int type;

    @Comment("权限名称")
    @Column(columnDefinition = "varchar(10) ")
    private String name;


    @Comment("权限描述")
    @Column(columnDefinition = "varchar(50) ")
    private String description;

    @Comment("父权限id")
    private Long parentId; 


    @Comment("权限值,通常为菜单相关或请求url")
    private String value;


    public int getType() {
        return type;
    }


    public void setType(int type) {
        this.type = type;
    }


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


    public Long getParentId() {
        return parentId;
    }


    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }


    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }

    
}

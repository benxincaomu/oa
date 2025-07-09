package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "permission")
public class Permission extends BaseEntity {
    public final static Integer TYPE_1 = 1;
    public final static Integer TYPE_2 = 2;
    public final static Integer TYPE_3 = 3;
    public final static Integer TYPE_4 = 4;
    @Comment("权限类型:1--目录菜单，2--页面菜单,3--请求url，4--页面控制")
    private Integer type;

    @Comment("权限名称")
    @Column(columnDefinition = "varchar(10) ")
    private String name;


    @Comment("权限描述")
    @Column(columnDefinition = "varchar(50) ")
    private String description;

    @Comment("父权限id,目录菜单可以无parentId或者以其他目录菜单为parentId，页面菜单必须以目录菜单为上级权限，页面控制必须以页面菜单为上级，请求链接的parentId是无效字段")
    private Long parentId; 


    @Comment("权限值,通常为菜单相关或请求url")
    private String value;

    @Transient
    private List<Permission> children;


  


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


    public List<Permission> getChildren() {
        return children;
    }


    public void setChildren(List<Permission> children) {
        this.children = children;
    }


    public Integer getType() {
        return type;
    }


    public void setType(Integer type) {
        this.type = type;
    }

    
}

package io.github.benxincaomu.oa.user;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.BaseEntity;
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
}

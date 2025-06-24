package io.github.benxincaomu.oa.user;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class Role extends BaseEntity {
    
    @Comment(value = "角色名称")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

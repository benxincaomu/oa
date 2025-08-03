package io.github.benxincaomu.oa.bussiness.organization;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Table(name = "department")
@Entity
@Comment("部门")
public class Department extends BaseEntity{


    @Comment("部门名称")
    private String name;

    @Comment("上级部门id")
    private Long parentId;

    @Comment("部门编码")
    private String code;

    @Comment("部门负责人id")
    private Long leaderUserId;

    

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Long getLeaderUserId() {
        return leaderUserId;
    }

    public void setLeaderUserId(Long leaderUserId) {
        this.leaderUserId = leaderUserId;
    }


    
    
}

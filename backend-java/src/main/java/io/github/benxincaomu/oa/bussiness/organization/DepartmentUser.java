package io.github.benxincaomu.oa.bussiness.organization;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "department_user")
public class DepartmentUser extends BaseEntity {
    @Comment("部门id")
    private Long departmentId;
    @Comment("用户id")
    private Long userId;
    public Long getDepartmentId() {
        return departmentId;
    }
    public void setDepartmentId(Long departmentId) {
        this.departmentId = departmentId;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
}

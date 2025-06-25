package io.github.benxincaomu.oa.bussiness.user;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_role")
public class UserRole  extends BaseEntity{
    @Comment("用户id")
    private Long userId;

    @Comment("角色id")
    private Long roleId;
}

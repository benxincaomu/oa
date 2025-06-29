package io.github.benxincaomu.oa.base.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
// import jakarta.persistence.SequenceGenerator;

@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @SequenceGenerator(name="squence",allocationSize = 1)
    @Comment("自增主键")
    private Long id;

    @Comment("租户id")
    private Long tenantId;

    @CreatedBy
    @Comment("创建人")
    private Long createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Comment("创建时间")
    @CreatedDate
    private LocalDateTime createAt; 


    @LastModifiedBy
    @Comment("更新人")
    private Long updateBy;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @LastModifiedDate
    @Comment("更新时间")
    private LocalDateTime updateAt;


    public Long getId() {
        return id;
    }


    public void setId(Long id) {
        this.id = id;
    }


    public Long getCreateBy() {
        return createBy;
    }


    public void setCreateBy(Long createBy) {
        this.createBy = createBy;
    }


    public LocalDateTime getCreateAt() {
        return createAt;
    }


    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }


    public Long getUpdateBy() {
        return updateBy;
    }


    public void setUpdateBy(Long updateBy) {
        this.updateBy = updateBy;
    }


    public LocalDateTime getUpdateAt() {
        /* if(updateAt == null){
            updateAt = LocalDateTime.now();
        } */
        return updateAt;
    }


    public void setUpdateAt(LocalDateTime updateAt) {
        this.updateAt = updateAt;
    }


    public Long getTenantId() {
        return tenantId;
    }


    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    
}

package io.github.benxincaomu.oa.base;

import java.time.LocalDateTime;

import org.hibernate.annotations.Comment;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Comment("自增主键")
    private Long id;

    @Comment("租户id")
    private Long tenantId;

    @Comment("创建人")
    private Long createBy;

    @Comment("创建时间")
    @Column(columnDefinition="timestamp default CURRENT_TIMESTAMP")
    private LocalDateTime createAt; 


    @Comment("更新人")
    private Long updateBy;


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
        if(updateAt == null){
            updateAt = LocalDateTime.now();
        }
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

package io.github.benxincaomu.oa.tenant;

import org.hibernate.annotations.Comment;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * 租户
 */
@Entity
@Table(name="tenant")
public class Tenant extends BaseEntity{

   

    @Column(columnDefinition = "varchar(20) ")
    @Comment("租户名称")
    private String tenantName;


    @Comment("是否启用")
    private boolean enable;



    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

   

    

    

}

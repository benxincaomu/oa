package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Map;

import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.benxincaomu.oa.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "flow_form_data_sample",indexes = {@Index(name = "flow_form_data_sample_create_by_index",columnList = "createBy")})
public class FlowForm extends BaseEntity{
    @Comment("流程发布ID")
    private Long publishId;

    @Comment("字段列表")
    @Column(name = "data",columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String,Object> data;
    
}

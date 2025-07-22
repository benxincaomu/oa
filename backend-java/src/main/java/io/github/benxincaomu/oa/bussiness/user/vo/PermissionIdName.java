package io.github.benxincaomu.oa.bussiness.user.vo;

import java.util.List;

import jakarta.persistence.Transient;

public class PermissionIdName {
    

 

    public PermissionIdName(Long id, String name, Long parentId, Integer type, String value) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.type = type;
        this.value = value;
    }

    

    private Long id;

    private String name;

    private Long parentId;

    private Integer type;

    private String value;

    @Transient
    List<PermissionIdName> children;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<PermissionIdName> getChildren() {
        return children;
    }

    public void setChildren(List<PermissionIdName> children) {
        this.children = children;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    
}

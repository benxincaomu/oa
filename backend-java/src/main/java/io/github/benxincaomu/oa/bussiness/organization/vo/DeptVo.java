package io.github.benxincaomu.oa.bussiness.organization.vo;

import java.util.List;


/**
 * 用于树形结构
 */
public class DeptVo {
    private Long id;
    private String name;

    private Long parentId;

    private String code;
    
    
    private List<DeptVo> children;

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

    public List<DeptVo> getChildren() {
        return children;
    }

    public void setChildren(List<DeptVo> children) {
        this.children = children;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

}

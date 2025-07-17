package io.github.benxincaomu.oa.bussiness.workflow;

public class EntityColumn {

    /**
     * 字段排序
     */
    private int sort;
    /**
     * 字段名
     */
    private String columnName;

    /**
     * 字段类型
     */
    private String columnType;

    /**
     * 枚举Id
     */
    private Long enumId;

    /**
     * 校验类型
     */
    private String[] validateTypes;

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getColumnType() {
        return columnType;
    }

    public void setColumnType(String columnType) {
        this.columnType = columnType;
    }

    public Long getEnumId() {
        return enumId;
    }

    public void setEnumId(Long enumId) {
        this.enumId = enumId;
    }

    public String[] getValidateTypes() {
        return validateTypes;
    }

    public void setValidateTypes(String[] validateTypes) {
        this.validateTypes = validateTypes;
    }
    

}

package io.github.benxincaomu.oa.bussiness.workflow_design;

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
     * 展示名称
     */
    private String label;

    /**
     * 字段类型
     */
    private String columnType;

    /**
     * 枚举Id
     */
    private Long enumId;

    /**
     * 字段是否显示在列表中
     */
    private boolean listAble;



    /**
     * 校验类型
     */
    private String[] validateTypes;

    /**
     * 数值类型的单位
     */
    private String unit;

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

    public boolean isListAble() {
        return listAble;
    }

    public void setListAble(boolean listAble) {
        this.listAble = listAble;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
    

    
}

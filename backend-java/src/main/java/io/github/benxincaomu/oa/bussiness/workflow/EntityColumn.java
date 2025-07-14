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

    
}

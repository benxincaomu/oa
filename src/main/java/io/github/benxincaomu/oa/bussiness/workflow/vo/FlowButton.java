package io.github.benxincaomu.oa.bussiness.workflow.vo;

public class FlowButton {

    

    public FlowButton(String name, String value) {
        this.name = name;
        this.value = value;
    }
    /**
     * 展示在标签上的名称
     */
    private String name;
    /**
     * outgoing flow id
     */
    private String value;
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }

    
}

package io.github.benxincaomu.oa.bussiness.workflow.vo;

import java.util.List;

import io.github.benxincaomu.oa.bussiness.workflow.FlowForm;
import io.github.benxincaomu.oa.bussiness.workflow_design.EntityColumn;

public class FlowFormDetailVo {

    private FlowForm flowForm;
    private List<EntityColumn> columns;

    private List<FlowButton> buttons;
    public FlowForm getFlowForm() {
        return flowForm;
    }
    public void setFlowForm(FlowForm flowForm) {
        this.flowForm = flowForm;
    }
    public List<EntityColumn> getColumns() {
        return columns;
    }
    public void setColumns(List<EntityColumn> columns) {
        this.columns = columns;
    }
    public List<FlowButton> getButtons() {
        return buttons;
    }
    public void setButtons(List<FlowButton> buttons) {
        this.buttons = buttons;
    }

    
}

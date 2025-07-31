package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.List;

import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.bussiness.workflow.vo.FlowFormDetailVo;
import io.github.benxincaomu.oa.bussiness.workflow_design.EntityColumn;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublish;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublishService;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 处理表单信息
 */
@RestController
@RequestMapping("/flowForm/{workbenchId}")
public class FlowFormController {

    @Resource
    private WorkbenchPublishService workbenchPublishService;

    @Resource
    private WorkbenchService workbenchService;

    @Resource
    private FlowFormService flowFormService;

    @GetMapping("getWorkbenchPublishById")
    public WorkbenchPublish getWorkbenchPublishById(@PathVariable("workbenchId") Long workbenchId) {
        return workbenchPublishService.getLastPublish(workbenchId);
    }

    @PostMapping
    public void saveFormData(@RequestBody FlowForm flowForm, @PathVariable("workbenchId") Long workbenchId) {

        flowFormService.saveFormData(flowForm, workbenchId, false);

    }

    @PostMapping("commit")
    public void commit(@RequestBody FlowForm flowForm, @PathVariable("workbenchId") Long workbenchId) {

        flowFormService.saveFormData(flowForm, workbenchId, true);

    }

    @GetMapping("listMyStart")
    public PagedModel<FlowForm> listMyStart(Integer currPage, Integer pageSize,
            @PathVariable("workbenchId") Long workbenchId) {
        return new PagedModel<>(flowFormService.listMyStart(currPage, pageSize, workbenchId));
    }

    /**
     * 获取我的待办
     * 
     * @param currPage    当前页
     * @param pageSize    分页大小
     * @param workbenchId 工作台id
     * @param starterId   工单发起人
     * @return
     */
    @GetMapping("listMyTodo")
    public PagedModel<FlowForm> listTodo(Integer currPage, Integer pageSize,
            @PathVariable("workbenchId") Long workbenchId, Long starterId) {
        return new PagedModel<>(flowFormService.listTodo(currPage, pageSize, workbenchId));
    }

    @GetMapping("{id}")
    public FlowForm getFlowForm(@PathVariable("workbenchId") Long workbenchId, @PathVariable("id") Long id) {
        return flowFormService.findById(id, workbenchId);

    }

    /**
     * 获取表单字段定义
     * 
     * @param param
     * @return
     */
    @GetMapping("getColumns/{formId}")
    public List<EntityColumn> getMethodName(@PathVariable("workbenchId") Long workbenchId,
            @PathVariable("formId") String formId) {

        return flowFormService.getColumns(workbenchId, formId);
    }

    /**
     * 表单详情
     * 
     * @param workbenchId 工作台id
     * @param formId      表单id
     * @return
     */
    @GetMapping("getFlowFormDetail/{formId}")
    public FlowFormDetailVo getFlowFormDetail(@PathVariable("workbenchId") Long workbenchId,
            @PathVariable("formId") Long formId) {

        return flowFormService.getFlowFormDetail(workbenchId, formId);
    }

}

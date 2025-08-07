package io.github.benxincaomu.oa.bussiness.workflow;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

/**
 * 处理流程信息
 */
@RestController
@RequestMapping("workflow/{workbenchId}")
public class WorkflowController {

    @Resource
    private WorkflowService workflowService;
    @PostMapping("approval")
    public void approval(@RequestBody FlowHistory flowHistory,@PathVariable("workbenchId") Long workbenchId) {
        workflowService.approval(flowHistory,workbenchId);

    }

}

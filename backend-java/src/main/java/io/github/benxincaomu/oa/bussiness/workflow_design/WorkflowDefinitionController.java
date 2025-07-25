package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.base.utils.Pair;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;



@RestController
@RequestMapping("/workflowDefinition")
public class WorkflowDefinitionController {

    /**
     * 任务分配类型
     */
    private static Pair[] ASSIGN_TYPES = new Pair[]{
            new Pair("assignee", "指定审批人"),
            new Pair("starterLeader", "发起人部门领导"),
            new Pair("assigneeDept", "按部门指定"),
    };

    @Resource
    private WorkflowDefinitionService workflowDefinitionService;

    @GetMapping("getByWorkbenchId/{workbenchId}")
    public WorkflowDefinition getByWorkbenchId(@PathVariable("workbenchId") Long workbenchId) {

        return workflowDefinitionService.getByWorkbenchId(workbenchId);
    }


    @PostMapping
    public Long save(@RequestBody WorkflowDefinition workflowDefinition) {

        return workflowDefinitionService.save(workflowDefinition);
    }
    

    @GetMapping("getAssignTypes")
    public Pair[] getAssignTypes() {
        return ASSIGN_TYPES;
    }
    

}

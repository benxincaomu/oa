package io.github.benxincaomu.oa.bussiness.workflow;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/workflowDefinition")
public class WorkflowDefinitionController {

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
    

}

package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.runtime.Execution;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.springframework.stereotype.Service;

import com.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
import io.github.benxincaomu.oa.bussiness.workflow.vo.FlowButton;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublishRepository;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Service
public class WorkflowService {
    @Resource
    private WorkbenchPublishRepository workbenchPublishRepository;

    @Resource
    private WorkbenchRepository workbenchRepository;
    @Resource
    private FlowFormRepository flowFormRepository;

    @Resource
    private FlowHistoryRepository flowHistoryRepository;

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private TaskService taskService;

    @Resource
    private ProcessEngine processEngine;

    @Resource
    private RepositoryService repositoryService;
    @Resource
    private UserRepository userRepository;
    @Resource
    private DepartmentUserRepository departmentUserRepository;

    @Transactional
    public void approval(FlowHistory flowHistory, Long workbenchId) {
        workbenchPublishRepository.findLatestByWorkbenchId(workbenchId).ifPresent(wp -> {
            userRepository.findById(JpaAuditorAware.getCurrentUserId()).ifPresent(user -> {
                flowHistory.setOperatorName(user.getName());
            });
            FlowForm flowForm = flowFormRepository.findOneById(flowHistory.getFlowFormId(), wp.getFlowFormTable());
            if (flowForm != null) {
                BpmnModelInstance modelInstance = repositoryService
                        .getBpmnModelInstance(wp.getWorkflowDeployId());
                SequenceFlow sequenceFlow = modelInstance.getModelElementById(flowHistory.getFlowId());
                flowHistory.setFlowName(sequenceFlow.getName());
                FlowNode source = sequenceFlow.getSource();
                if (source instanceof UserTask) {
                    UserTask userTask = (UserTask) source;
                    Task task = taskService.createTaskQuery()
                            .processInstanceId(flowForm.getProcessId())
                            .taskAssignee(JpaAuditorAware.getCurrentUserId().toString())
                            .taskDefinitionKey(userTask.getId())
                            .active()
                            .singleResult();
                    if (task == null) {
                        Optional<Long> deptId = departmentUserRepository.findDeptIdByUserId(JpaAuditorAware.getCurrentUserId());
                        task = taskService.createTaskQuery()
                                .processInstanceId(flowForm.getProcessId())
                                .taskCandidateGroup(deptId.orElse(0L).toString())
                                .taskDefinitionKey(userTask.getId())
                                .active()
                                .singleResult();
                    }
                    Asserts.isTrue(task != null, OaResponseCode.FLOW_TASK_NOT_EXIST);
                    // 认领任务
                    taskService.claim(task.getId(), JpaAuditorAware.getCurrentUserId().toString());
                    // 设定任务按sequenceFlow为流向
                    String executionId = task.getExecutionId();
                    runtimeService.createProcessInstanceModification(flowForm.getProcessId())
                            .cancelActivityInstance(executionId)
                            .startBeforeActivity(sequenceFlow.getTarget().getId())
                            .execute();
                    ;

                }

                flowHistoryRepository.save(flowHistory, wp.getFlowHistoryTable());
            }


        });
        ;
    }

}

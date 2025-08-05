package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
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


    @Resource
    private FlowFormAssigneeRepository flowFormAssigneeRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Transactional
    public void approval(FlowHistory flowHistory, Long workbenchId) {
        // logger.info("approval");
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
                flowHistory.setNodeName(sequenceFlow.getSource().getName());
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
                    flowHistoryRepository.save(flowHistory, wp.getFlowHistoryTable());
                    Asserts.isTrue(task != null, OaResponseCode.FLOW_TASK_NOT_EXIST);
                    // 删除待办的关联数据
                    flowFormAssigneeRepository.delete(flowForm.getId(), wp.getFlowFormAssigneeTable());
                    // 认领任务
                    taskService.claim(task.getId(), JpaAuditorAware.getCurrentUserId().toString());
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("flowId", flowHistory.getFlowId());
                    taskService.complete(task.getId(),variables);

                }

            }


        });
        ;
    }

}

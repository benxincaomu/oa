package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakDirectoryService;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublish;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublishRepository;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Service("userTaskCreateListener")
public class UserTaskCreateListener implements JavaDelegate {

    @Resource
    private KeycloakDirectoryService directoryService;

    @Resource
    private RuntimeService runtimeService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private WorkbenchRepository workbenchRepository;

    @Resource
    private WorkbenchPublishRepository workbenchPublishRepository;

    @Resource
    private FlowFormAssigneeRepository flowFormAssigneeRepository;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Transactional
    @Override
    public void execute(DelegateExecution execution) throws Exception {
         FlowElement bpmnModelElementInstance = execution.getBpmnModelElementInstance();

        if (bpmnModelElementInstance != null && bpmnModelElementInstance instanceof UserTask) {
            UserTask userTask = (UserTask) bpmnModelElementInstance;
            String processDefinitionId = execution.getProcessDefinitionId();

            WorkbenchPublish workbenchPublish = workbenchPublishRepository.findByWorkflowDeployId(processDefinitionId)
                    .get();
            FlowFormAssignee flowFormAssignee = new FlowFormAssignee();
            flowFormAssignee.setWorkbenchId(workbenchPublish.getWorkbenchId());

            // flowFormAssignee.setFlowFormId(delegateTask.getExecution().getProcessInstanceId());
            String camundaAssignee = userTask.getCamundaAssignee();
            if (Objects.equals(camundaAssignee, "${starterId}") || Objects.equals(camundaAssignee, "${starterLeader}")) {
                // 发起人部门领导审批：数字 ID 通过 Keycloak 组的 leader_user_id 属性解析
                Object starterIdValue = execution.getVariable(FlowConsts.STARTER_ID);
                if (starterIdValue != null) {
                    Long starterId = Long.valueOf(starterIdValue.toString());
                    Long leaderId = directoryService
                            .findLeaderNumericIdForUser(starterId, JpaAuditorAware.getCurrentTenantId())
                            .orElse(null);
                    Long assigneeId = leaderId == null ? starterId : leaderId;
                    userTask.setCamundaAssignee(String.valueOf(assigneeId));
                    flowFormAssignee.setAssignee(assigneeId);
                }
            } else if (camundaAssignee != null) {
                flowFormAssignee.setAssignee(Long.valueOf(camundaAssignee));
            }
            String camundaCandidateGroups = userTask.getCamundaCandidateGroups();
            if (camundaCandidateGroups != null) {
                flowFormAssignee.setCandidateGroups(Arrays.stream(camundaCandidateGroups.split(",")).map(Long::valueOf)
                        .collect(Collectors.toList()));
            }
            flowFormAssignee.setActived(true);
            flowFormAssignee.setFlowFormId((Long) execution.getVariable(FlowConsts.FLOW_FORM_ID));
            flowFormAssigneeRepository.save(flowFormAssignee, workbenchPublish.getFlowFormAssigneeTable());
        }
    }
}

package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.bussiness.user.UserService;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublish;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublishRepository;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Service("userTaskCreateListener")
public class UserTaskCreateListener implements JavaDelegate {

    @Resource
    private UserService userService;

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
        UserTask userTask = (UserTask) execution.getBpmnModelElementInstance();

        if (userTask != null) {
            String processDefinitionId = execution.getProcessDefinitionId();

            WorkbenchPublish workbenchPublish = workbenchPublishRepository.findByWorkflowDeployId(processDefinitionId)
                    .get();
            FlowFormAssignee flowFormAssignee = new FlowFormAssignee();
            flowFormAssignee.setWorkbenchId(workbenchPublish.getWorkbenchId());

            // flowFormAssignee.setFlowFormId(delegateTask.getExecution().getProcessInstanceId());
            String camundaAssignee = userTask.getCamundaAssignee();
            if (Objects.equals(camundaAssignee, "${starterId}")) {
                // 获取发起人id
                String starterId = (String) execution.getVariable("starterId");
                if (starterId != null) {
                    userTask.setCamundaAssignee(userService.findLeaderId(Long.valueOf(starterId)) + "");
                    flowFormAssignee.setAssignee(Long.valueOf(starterId));
                }
            } else if (camundaAssignee != null) {
                flowFormAssignee.setAssignee(Long.valueOf(camundaAssignee));
            }
            String camundaCandidateGroups = userTask.getCamundaCandidateGroups();
            if (camundaCandidateGroups != null) {
                flowFormAssignee.setCandidateGroups(Arrays.stream(camundaCandidateGroups.split(",")).map(Long::valueOf)
                        .collect(Collectors.toList()));
            }
            Map<String, Object> variables = execution.getVariables();
            flowFormAssignee.setActived(true);
            flowFormAssignee.setFlowFormId((Long) execution.getVariable(FlowConsts.FLOW_FORM_ID));
            flowFormAssigneeRepository.save(flowFormAssignee, workbenchPublish.getFlowFormAssigneeTable());
        }
    }
}

package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Collection;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.bussiness.user.UserService;
import jakarta.annotation.Resource;

@Service("userTaskCreateListener")
public class UserTaskCreateListener implements TaskListener {

    @Resource
    private UserService userService;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void notify(DelegateTask delegateTask) {
        UserTask userTask = (UserTask) delegateTask.getBpmnModelElementInstance();

        if (userTask != null) {
            String camundaAssignee = userTask.getCamundaAssignee();
            if (Objects.equals(camundaAssignee, "${starterId}")) {
                // 获取发起人id
                String starterId = (String) delegateTask.getVariable("starterId");
                if (starterId != null) {
                    userTask.setCamundaAssignee(userService.findLeaderId(Long.valueOf(starterId))+"");
                }
            }
        }
    }

}

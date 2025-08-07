package io.github.benxincaomu.oa.bussiness.workflow;

import java.time.Duration;
import java.util.Objects;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.impl.history.event.HistoryEvent;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowElement;
import org.camunda.bpm.spring.boot.starter.event.ExecutionEvent;
import org.camunda.bpm.spring.boot.starter.event.TaskEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Component
public class WorkFlowListener {

    @Resource
    private RuntimeService runtimeService;

    @Resource
    private FlowHistoryRepository flowHistoryRepository;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @EventListener
    public void onTaskEvent(DelegateTask taskDelegate) {
        // logger.info("DelegateTask:{}", taskDelegate.getExecutionId());
        // handle mutable task event
    }

    @EventListener
    public void onTaskEvent(TaskEvent taskEvent) {
        // logger.info("taskEvent:{}", taskEvent.getExecutionId());
    }

    @EventListener
    public void onExecutionEvent(DelegateExecution executionDelegate) {
        // handle mutable execution event
        // logger.info("executionDelegate:{}",
        // executionDelegate.getCurrentActivityId());
    }

    @EventListener
    @Transactional
    public void onExecutionEvent(ExecutionEvent executionEvent) {
        // handle immutable execution event
        BpmnModelInstance modelInstance = repositoryService
                .getBpmnModelInstance(executionEvent.getProcessDefinitionId());
        FlowElement flowElement = modelInstance.getModelElementById(executionEvent.getCurrentActivityId());
        if (Objects.equals("end", executionEvent.getEventName())
                && Objects.equals("endEvent", flowElement.getElementType().getTypeName())
                && redisTemplate.opsForValue().setIfAbsent(executionEvent.getId(), "1",
                        Duration.ofSeconds(30L)) == true) {

            FlowHistory flowHistory = new FlowHistory();
            VariableMap variables = runtimeService.getVariablesTyped(executionEvent.getProcessInstanceId());
            flowHistory.setFlowFormId(variables.getValue(FlowConsts.FLOW_FORM_ID, Long.class));
            flowHistory.setNodeName(executionEvent.getCurrentActivityName());
            flowHistory.setFlowName("流程结束");

            String historyTable = variables.getValue(FlowConsts.FLOW_HISTORY_TABLE, String.class);
            flowHistoryRepository.save(flowHistory, historyTable);

        }
    }

    @EventListener
    public void onHistoryEvent(HistoryEvent historyEvent) {
        // handle history event
        // logger.info("historyEvent:{}", historyEvent.getExecutionId());
    }
}

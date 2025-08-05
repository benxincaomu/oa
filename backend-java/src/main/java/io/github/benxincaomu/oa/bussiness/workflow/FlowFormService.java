package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.bpmn.instance.StartEvent;
import org.camunda.bpm.model.bpmn.instance.UserTask;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.notry.exception.CommonException;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.web.OaResponseCode;
import io.github.benxincaomu.oa.bussiness.organization.DepartmentUserRepository;
import io.github.benxincaomu.oa.bussiness.workflow.vo.FlowButton;
import io.github.benxincaomu.oa.bussiness.workflow.vo.FlowFormDetailVo;
import io.github.benxincaomu.oa.bussiness.workflow_design.EntityColumn;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublish;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchPublishRepository;
import io.github.benxincaomu.oa.bussiness.workflow_design.WorkbenchRepository;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

/**
 * 流程表单处理
 */
@Service
public class FlowFormService {
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
    private DepartmentUserRepository departmentUserRepository;

    @Transactional
    public void saveFormData(FlowForm flowForm, Long workbenchId, boolean commit) {
        WorkbenchPublish wp = workbenchPublishRepository.findLatestByWorkbenchId(workbenchId)
                .orElseThrow(() -> new CommonException(OaResponseCode.WORKBENCH_PUBLISH_NOT_EXIST));
        // flowForm.setPublishId(wp.getId());
        // 填充信息
        flowForm.setWorkbenchId(workbenchId);
        if (flowForm.getId() == null) {
            flowForm.setId(flowFormRepository.getNextId(wp.getFlowFormTable()));
        }

        if (commit) {
            // 发起流程
            Map<String, Object> variables = new HashMap<>();
            variables.put(FlowConsts.STARTER_ID, JpaAuditorAware.getCurrentUserId());
            variables.put(FlowConsts.DATA, flowForm.getData());
            variables.put(FlowConsts.FLOW_FORM_ASSIGNEE_TABLE, wp.getFlowFormAssigneeTable());
            variables.put(FlowConsts.FLOW_HISTORY_TABLE, wp.getFlowHistoryTable());
            variables.put(FlowConsts.FLOW_FORM_ID, flowForm.getId());
            // variables.put(FlowConsts.WORKBENCH_PUBLISH, wp);
            ProcessInstance process = runtimeService.startProcessInstanceById(wp.getWorkflowDeployId(), variables);
            
            flowForm.setProcessId(process.getId());
            // flowForm.setDeployName(workbench.getWorkbenchKey());
            flowFormRepository.save(flowForm, workbenchId, wp.getFlowFormTable());
            // 保存日志
            FlowHistory flowHistory = new FlowHistory();
            flowHistory.setFlowFormId(flowForm.getId());
            BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(process.getProcessDefinitionId());
            Collection<StartEvent> startEvents = bpmnModelInstance.getModelElementsByType(StartEvent.class);
            if(startEvents != null && startEvents.size() > 0){
                StartEvent startEvent = startEvents.iterator().next();
                flowHistory.setNodeName(startEvent.getName() == null? "开始":startEvent.getName());
            }
            flowHistoryRepository.save(flowHistory, wp.getFlowHistoryTable());

        } else {
            flowFormRepository.save(flowForm, workbenchId, wp.getFlowFormTable());
        }
    }

    public Page<FlowForm> listMyStart(Integer currPage, Integer pageSize, Long workbenchId) {
        return workbenchRepository.findById(workbenchId)
                .flatMap(workbench -> workbenchPublishRepository.findLatestByWorkbenchId(workbenchId)
                        .map(wp -> flowFormRepository.myStart(wp.getFlowFormTable(), workbench.getId(),
                                JpaAuditorAware.getCurrentUserId(), currPage, pageSize)))
                .orElse(Page.empty());
    }

    public FlowForm findById(Long id, Long workbenchId) {
        return workbenchPublishRepository.findLatestByWorkbenchId(workbenchId)
                .map(wp -> flowFormRepository.findOneById(id, wp.getFlowFormTable()))
                .orElseThrow(() -> new CommonException(OaResponseCode.WORKBENCH_PUBLISH_NOT_EXIST));
    }

    public List<EntityColumn> getColumns(Long workbenchId, String formId) {
        workbenchRepository.findById(workbenchId).ifPresent(workbench -> {

        });
        return null;
    }

    public FlowFormDetailVo getFlowFormDetail(Long workbenchId, Long id) {
        FlowFormDetailVo vo = new FlowFormDetailVo();

        return workbenchRepository.findById(workbenchId)
                .flatMap(workbench -> workbenchPublishRepository.findLatestByWorkbenchId(workbenchId)
                        .map(wp -> {
                            FlowForm flowForm = flowFormRepository.findOneById(id, wp.getFlowFormTable());
                            // 已发起的流程填充
                            if (flowForm.isStarted()) {
                                // 流程历史

                                flowForm.setFlowHistories(
                                        flowHistoryRepository.findAll(flowForm.getId(), wp.getFlowHistoryTable()));
                                // 当前的可操作的按钮
                                List<Task> currentTasks = taskService.createTaskQuery()
                                        .processInstanceId(flowForm.getProcessId())
                                        .active() // 确保只获取活跃的任务
                                        .taskAssignee(JpaAuditorAware.getCurrentUserId() + "")
                                        .list();
                                Task task = null;
                                if (currentTasks.size() > 0) {
                                    // TODO 先处理只有一个当前任务的情况
                                    task = currentTasks.get(0);

                                } else {
                                    // 查询当前登陆人的部门
                                    Long deptId = departmentUserRepository
                                            .findDeptIdByUserId(JpaAuditorAware.getCurrentUserId()).orElse(0L);
                                    currentTasks = taskService.createTaskQuery()
                                            .processInstanceId(flowForm.getProcessId())
                                            .active() // 确保只获取活跃的任务
                                            .taskCandidateGroup(deptId + "")
                                            .list();
                                    if (currentTasks.size() > 0) {
                                        task = currentTasks.get(0);
                                    }
                                }
                                if (task != null) {
                                    String taskDefinitionKey = task.getTaskDefinitionKey();
                                    BpmnModelInstance modelInstance = repositoryService
                                            .getBpmnModelInstance(wp.getWorkflowDeployId());
                                    UserTask userTask = modelInstance.getModelElementById(taskDefinitionKey);
                                    if (userTask != null) {
                                        Collection<SequenceFlow> outgoing = userTask.getOutgoing();
                                        List<FlowButton> buttons = new ArrayList<>();
                                        for (SequenceFlow sequenceFlow : outgoing) {
                                            String name = sequenceFlow.getName();
                                            String value = sequenceFlow.getId();
                                            buttons.add(new FlowButton(name, value));
                                        }
                                        vo.setButtons(buttons);

                                    }
                                }

                            }

                            if (flowForm != null && flowForm.getPublishId() != null) {
                                workbenchPublishRepository.findById(flowForm.getPublishId())
                                        .ifPresent(wp1 -> {
                                            if (wp1.getEntityDefinition() != null) {
                                                vo.setColumns(wp1.getEntityDefinition().getColumns());
                                            }
                                            vo.setFlowForm(flowForm);
                                        });
                            }
                            return vo;
                        }))
                .orElse(vo);
    }

    public Page<FlowForm> listTodo(Integer currPage, Integer pageSize, Long workbenchId, Long starterId) {
        Long currentUserId = JpaAuditorAware.getCurrentUserId();
        Long deptId = departmentUserRepository.findDeptIdByUserId(currentUserId).orElse(0L);

        return workbenchPublishRepository.findLatestByWorkbenchId(workbenchId)
                .map(wp -> flowFormRepository.myToDo(
                        wp.getFlowFormTable(),
                        wp.getFlowFormAssigneeTable(),
                        workbenchId,
                        currentUserId,
                        deptId,
                        starterId,
                        currPage,
                        pageSize))
                .orElse(Page.empty());
    }

    public Page<FlowForm> listMyDone(Integer currPage, Integer pageSize, Long workbenchId, Long starterId) {
        Long currentUserId = JpaAuditorAware.getCurrentUserId();

        return workbenchPublishRepository.findLatestByWorkbenchId(workbenchId)
                .map(wp -> flowFormRepository.myDone(
                        wp.getFlowFormTable(),
                        wp.getFlowHistoryTable(),
                        workbenchId,
                        currentUserId,
                        starterId,
                        currPage,
                        pageSize))
                .orElse(Page.empty());
    }
}

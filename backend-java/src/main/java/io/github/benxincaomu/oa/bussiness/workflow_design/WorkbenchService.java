package io.github.benxincaomu.oa.bussiness.workflow_design;

import java.util.UUID;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Data;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.user.Permission;
import io.github.benxincaomu.oa.bussiness.user.PermissionRepository;
import io.github.benxincaomu.oa.bussiness.user.PermissionService;
import io.github.benxincaomu.oa.bussiness.workflow.DataTableService;
import io.github.benxincaomu.oa.bussiness.workflow_design.vo.PublishVo;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@Service
public class WorkbenchService {

    @Resource
    private WorkbenchRepository workbenchRepository;

    @Resource
    private EntityDefinitionService entityDefinitionService;

    @Resource
    private WorkflowDefinitionService workflowDefinitionService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private WorkbenchPublishRepository workbenchPublishRepository;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private DataTableService dataTableService;

    @Resource
    private EntityManager entityManager;

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public Workbench getWorkbenchById(Long id) {
        return workbenchRepository.findOneByIdAndTenantId(id, JpaAuditorAware.getCurrentTenantId());
    }

    public Workbench createWorkbench(Workbench workbench) {
        if (workbench.getWorkbenchKey() == null) {
            workbench.setWorkbenchKey(UUID.randomUUID().toString());
        }
        return workbenchRepository.save(workbench);
    }

    @Transactional
    public void publish(PublishVo vo) {
        workbenchRepository.findById(vo.getWorkbenchId()).ifPresent(workbench -> {

            // 保存beand定义
            EntityDefinition entityDefinition = entityDefinitionService.insert(vo.getEntityDefinition());
            // 保存流程定义
            workflowDefinitionService.save(vo.getWorkflowDefinition());
            if (workbench.getWorkbenchKey() == null) {
                String workbenchKey = UUID.randomUUID().toString();
                workbench.setWorkbenchKey(workbenchKey);
                workbenchRepository.save(workbench);
            }
            // 发布流程
            Deployment deploy = repositoryService.createDeployment()
            .name(workbench.getWorkbenchKey())
                    .addString(workbench.getWorkbenchKey()+ ".bpmn", vo.getWorkflowDefinition().getFlowDefinition())
                    .deploy();
            // entityManager.flush();
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                    .deploymentId(deploy.getId())
                    // .processDefinitionName(workbench.getWorkbenchKey())
                    
                    // .processDefinitionKey(workbench.getWorkbenchKey())
                    .singleResult();
            WorkbenchPublish publish = workbenchPublishRepository.findLatestByWorkbenchId(workbench.getId())
                    .orElseGet(() -> {
                        WorkbenchPublish publishNew = new WorkbenchPublish();
                        publishNew.setWorkbenchId(workbench.getId());
                        dataTableService.startSelectTable();
                        publishNew.setFlowFormTable(dataTableService.selectFormTable());
                        publishNew.setFlowHistoryTable(dataTableService.selectHistoryTable());
                        publishNew.setFlowFormAssigneeTable(dataTableService.selectFlowFormAssigineeTable());
                        dataTableService.endSelectTable();
                        publishNew.setVersion(0L);
                        return publishNew;
                    });
            entityManager.detach(publish);
            ;
            WorkbenchPublish publishNew = new WorkbenchPublish();

            publishNew.setWorkbenchId(workbench.getId());
            publishNew.setWorkflowDeployId(processDefinition.getId());
            publishNew.setVersion(publish.getVersion() + 1L);
            publishNew.setEntityDefinition(entityDefinition);
            publishNew.setFlowFormTable(publish.getFlowFormTable());
            publishNew.setFlowFormAssigneeTable(publish.getFlowFormAssigneeTable());
            publishNew.setFlowHistoryTable(publish.getFlowHistoryTable());

            workbenchPublishRepository.save(publishNew);

            if (publishNew.getVersion() == 1L) {

                // 创建菜单
                Permission parentPermission = new Permission();
                parentPermission.setName(workbench.getName());
                parentPermission.setType(Permission.TYPE_1);
                parentPermission.setValue("/app/workflow/" + publish.getWorkbenchId());

                permissionService.insert(parentPermission);


                // 我启动的
                Permission myStart = new Permission();
                myStart.setName("我发起的");
                myStart.setType(Permission.TYPE_2);
                myStart.setValue("/app/workflow/" + publish.getWorkbenchId() + "/myStart");
                myStart.setParentId(parentPermission.getId());
                permissionService.insert(myStart);
                ;

                // 我的待办
                Permission myTodo = new Permission();
                myTodo.setName("我的待办");
                myTodo.setType(Permission.TYPE_2);
                myTodo.setValue("/app/workflow/" + publish.getWorkbenchId() + "/myTodo");
                myTodo.setParentId(parentPermission.getId());
                permissionService.insert(myTodo);

                // 我的已办
                Permission myDone = new Permission();
                myDone.setName("我的已办");
                myDone.setType(Permission.TYPE_2);
                myDone.setValue("/app/workflow/" + publish.getWorkbenchId() + "/myDone");
                myDone.setParentId(parentPermission.getId());
                permissionService.insert(myDone);

                // url权限
                // 处理表单信息
                Permission urlPermission1 = new Permission();
                urlPermission1.setName("处理表单信息url权限");
                urlPermission1.setType(Permission.TYPE_3);
                urlPermission1.setValue("/flowForm/" + publish.getWorkbenchId() + "/**");
                urlPermission1.setParentId(parentPermission.getId());
                permissionService.insert(urlPermission1);

                // 处理流程数据
                Permission urlPermission2 = new Permission();
                urlPermission2.setName("处理流程数据url权限");
                urlPermission2.setType(Permission.TYPE_3);
                urlPermission2.setValue("/workflow/" + publish.getWorkbenchId() + "/**");
                urlPermission2.setParentId(parentPermission.getId());
                permissionService.insert(urlPermission2);

            }

        });
        ;
    }

    public Page<Workbench> list(String name, Integer currPage, Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null ? 0 : currPage - 1, pageSize == null ? 20 : pageSize);
        Workbench workbench = new Workbench();
        ExampleMatcher matcher = ExampleMatcher.matching();
        if (name != null && !name.isEmpty()) {
            matcher.withMatcher("name", m -> m.exact());
            workbench.setName(name);
        }
        Example<Workbench> example = Example.of(workbench, matcher);
        return workbenchRepository.findAll(example, page);
    }
}

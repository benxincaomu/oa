package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsProperties.Data;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.user.Permission;
import io.github.benxincaomu.oa.bussiness.user.PermissionService;
import io.github.benxincaomu.oa.bussiness.workflow.DataTableService;
import io.github.benxincaomu.oa.bussiness.workflow_design.vo.PublishVo;
import jakarta.annotation.Resource;
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

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public Workbench getWorkbenchById(Long id) {
        return workbenchRepository.findOneByIdAndTenantId(id, JpaAuditorAware.getCurrentTenantId());
    }

    public Workbench createWorkbench(Workbench workbench) {
        workbench.setId(null);
        return workbenchRepository.save(workbench);
    }

    @Transactional
    public void publish(PublishVo vo){
        workbenchRepository.findById(vo.getWorkbenchId()).ifPresent(workbench -> {

            // 保存beand定义
            EntityDefinition entityDefinition = entityDefinitionService.insert(vo.getEntityDefinition());
            //保存流程定义
            workflowDefinitionService.save(vo.getWorkflowDefinition());
            // 发布流程
            Deployment deploy = repositoryService.createDeployment().name(workbench.getWorkbenchKey()).addString(workbench.getName(), vo.getWorkflowDefinition().getFlowDefinition()).deploy();

            WorkbenchPublish publish = workbenchPublishRepository.findLatestByWorkbenchId(workbench.getId()).orElseGet(()->{
                WorkbenchPublish publishNew = new WorkbenchPublish();
                publishNew.setWorkbenchId(workbench.getId());
                publishNew.setFlowFormTable(dataTableService.selectFormTable());
                publishNew.setFlowHistoryTable(dataTableService.selectHistoryTable());
                publishNew.setVersion(0L);
                return publishNew;
            });
            WorkbenchPublish publishNew = new WorkbenchPublish();

            publishNew.setWorkbenchId(workbench.getId());
            publishNew.setWorkflowDeployId(deploy.getId());
            publishNew.setVersion(publish.getVersion() + 1L);
            publishNew.setEntityDefinition(entityDefinition);
            publishNew.setFlowFormTable(publish.getFlowFormTable());
            publishNew.setFlowHistoryTable(publish.getFlowHistoryTable());
            workbenchPublishRepository.save(publishNew);

            if(publish.getVersion() == 1L){
                
                // 创建菜单
                Permission parentPermission = new Permission();
                parentPermission.setName(workbench.getName());
                parentPermission.setType(Permission.TYPE_1);
                parentPermission.setValue("/app/workflow/"+publish.getWorkbenchId());
    
                permissionService.insert(parentPermission);
    
                // 新建表单
                Permission start = new Permission();
                start.setName("新建表单");
                start.setType(Permission.TYPE_2);
                start.setValue("/app/workflow/"+publish.getWorkbenchId()+"/start");
    
                //我启动的
                Permission myStart = new Permission();
                myStart.setName("我发起的");
                myStart.setType(Permission.TYPE_2);
                myStart.setValue("/app/workflow/"+publish.getWorkbenchId()+"/myStart");
                myStart.setParentId(parentPermission.getId());
                permissionService.insert(myStart);
                ;
    
                // 我的待办
                Permission myTodo = new Permission();
                myTodo.setName("我的待办");
                myTodo.setType(Permission.TYPE_2);
                myTodo.setValue("/app/workflow/"+publish.getWorkbenchId()+"/myTodo");
                myTodo.setParentId(parentPermission.getId());
                permissionService.insert(myTodo);
    
                // 我的已办
                Permission myDone = new Permission();
                myDone.setName("我的已办");
                myDone.setType(Permission.TYPE_2);
                myDone.setValue("/app/workflow/"+publish.getWorkbenchId()+"/myDone");
                myDone.setParentId(parentPermission.getId());
                permissionService.insert(myDone);
    
                // url权限
                Permission urlPermission = new Permission();
                urlPermission.setName("url权限");
                urlPermission.setType(Permission.TYPE_3);
                urlPermission.setValue("/app/workflow/**/"+publish.getWorkbenchId());
                urlPermission.setParentId(parentPermission.getId());
            }

            
        });;
    }


    public Page<Workbench> list(String name, Integer currPage, Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null ? 0 : currPage - 1, pageSize==null?20:pageSize);
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

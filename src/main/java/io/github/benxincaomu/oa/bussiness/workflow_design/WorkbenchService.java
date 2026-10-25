package io.github.benxincaomu.oa.bussiness.workflow_design;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakAdminClient;
import io.github.benxincaomu.oa.base.security.KeycloakAuthorizationService;
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
    private KeycloakAuthorizationService authorizationService;

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
                    .addString(workbench.getWorkbenchKey() + ".bpmn", vo.getWorkflowDefinition().getFlowDefinition())
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

            // 菜单/接口资源在 Keycloak 中注册，默认授权 oa-admin；失败则发布失败
            registerAuthorizationResources(workbench);
        });
        ;
    }

    /**
     * 删除工作台：先回收其 Keycloak 菜单/接口资源，再删除本地工作台记录。
     * 已发布的流程数据与历史保留。
     */
    @Transactional
    public void delete(Long workbenchId) {
        workbenchRepository.findById(workbenchId).ifPresent(workbench -> {
            if (workbench.getWorkbenchKey() != null) {
                authorizationService.deleteWorkbenchResources(JpaAuditorAware.getCurrentTenantId(),
                        workbench.getWorkbenchKey());
            }
            workbenchRepository.delete(workbench);
        });
    }

    /**
     * 把工作台的菜单与接口注册为 Keycloak 授权资源，并默认授权给管理员角色。
     * 每次发布都 upsert，因此重试不会产生重复资源。
     */
    private void registerAuthorizationResources(Workbench workbench) {
        Long tenantId = JpaAuditorAware.getCurrentTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("发布工作台需要 OA 租户上下文");
        }
        String workbenchKey = workbench.getWorkbenchKey();
        String menuGroup = authorizationService.ensureMenuGroup(tenantId, "wb:" + workbenchKey,
                workbench.getName(), null, 100);
        String baseRoute = "/app/workflow/" + workbench.getId();
        List<String> resourceKeys = new ArrayList<>();
        resourceKeys.add(menuGroup);
        resourceKeys.add(authorizationService.ensureMenuPage(tenantId, baseRoute + "/myStart", "我发起的",
                menuGroup, 0));
        resourceKeys.add(authorizationService.ensureMenuPage(tenantId, baseRoute + "/myTodo", "我的待办",
                menuGroup, 1));
        resourceKeys.add(authorizationService.ensureMenuPage(tenantId, baseRoute + "/myDone", "我的已办",
                menuGroup, 2));
        resourceKeys.add(authorizationService.ensureApiResource(tenantId,
                "/flowForm/" + workbench.getId() + "/**", workbench.getName() + "表单接口", workbenchKey));
        resourceKeys.add(authorizationService.ensureApiResource(tenantId,
                "/workflow/" + workbench.getId() + "/**", workbench.getName() + "流程接口", workbenchKey));
        authorizationService.grantToRole(KeycloakAdminClient.OA_ADMIN_CLIENT_ROLE, resourceKeys);
        logger.info("Registered Keycloak authorization resources for workbench {} (key={})",
                workbench.getId(), workbenchKey);
    }

    public Page<Workbench> list(String name, Integer currPage, Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null || currPage <= 0 ? 0 : currPage - 1,
                pageSize == null ? 20 : pageSize);
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

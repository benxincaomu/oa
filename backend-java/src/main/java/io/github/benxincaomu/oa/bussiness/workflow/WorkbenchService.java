package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Optional;

import javax.swing.text.html.parser.Entity;

import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.slf4j.Logger;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.workflow.vo.PublishVo;
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
    private WorkbenchPublishRepository workbenchPublishRepository;

    @Resource
    private RepositoryService repositoryService;

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
            Deployment deploy = repositoryService.createDeployment().addString(workbench.getName(), vo.getWorkflowDefinition().getFlowDefinition()).deploy();

            WorkbenchPublish publish = workbenchPublishRepository.findLatestByWorkbenchId(workbench.getId()).orElseGet(()->{
                WorkbenchPublish publishNew = new WorkbenchPublish();
                publishNew.setVersion(0L);
                return publishNew;
            });

            publish.setWorkbenchId(workbench.getId());
            publish.setWorkflowDeployId(deploy.getId());
            publish.setVersion(publish.getVersion() + 1L);
            publish.setEntityDefinition(entityDefinition);
            
            ;
            
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

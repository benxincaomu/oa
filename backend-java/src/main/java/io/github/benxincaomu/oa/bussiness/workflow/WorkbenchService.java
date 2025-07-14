package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Optional;

import org.slf4j.Logger;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

@Service
public class WorkbenchService {

    @Resource
    private WorkbenchRepository workbenchRepository;

    private final Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    public Workbench getWorkbenchById(Long id) {
        return workbenchRepository.findOneByIdAndTenantId(id, JpaAuditorAware.getCurrentTenantId());
    }

    public Workbench createWorkbench(Workbench workbench) {
        workbench.setId(null);
        return workbenchRepository.save(workbench);
    }

    /**
     * TODO  确定版本 
     */
    @Transactional
    public void buildVersion(Workbench workbench) {
        if (workbench.getId() == null) {
            workbench.setVersion(1L);
        } else {
            Optional<Workbench> w = workbenchRepository.findById(workbench.getId());
            w.ifPresent((wb) -> workbench.setVersion(wb.getVersion() == null ? 1 : wb.getVersion() + 1));
        }

        workbenchRepository.save(workbench);
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

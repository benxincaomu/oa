package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.benxincaomu.oa.bussiness.workflow_design.vo.PublishVo;
import jakarta.annotation.Resource;

@RestController
@RequestMapping("/workbench")
public class WorkbenchController {
    @Resource
    private WorkbenchService workbenchService;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @GetMapping("list")
    public PagedModel<Workbench> list(String name, Integer currPage, Integer pageSize) {
        return new PagedModel<>(workbenchService.list(name, currPage, pageSize));
    }

    @GetMapping("{id}")
    public Workbench getById(@PathVariable("id") Long id) {
        return workbenchService.getWorkbenchById(id);
    }

    @PostMapping
    public Workbench createWorkbench(@RequestBody Workbench workbench) {
        return workbenchService.createWorkbench(workbench);
    }

    @PostMapping("publish")
    public void publicWorkbench(@RequestBody PublishVo publishVo) {
        workbenchService.publish(publishVo);
    }
}

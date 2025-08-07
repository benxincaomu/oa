package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

@Service
public class WorkbenchPublishService {

    @Resource
    private WorkbenchPublishRepository workbenchPublishRepository;

    @Resource
    private WorkbenchRepository workbenchRepository;

  

    /**
     * 获取工作台的已发布版本
     * 
     * @param workbenchId 工作流id
     * @return
     */
    public WorkbenchPublish getLastPublish(Long workbenchId) {
        return workbenchPublishRepository.findLatestByWorkbenchId(workbenchId).orElse(null);
    }

 
}

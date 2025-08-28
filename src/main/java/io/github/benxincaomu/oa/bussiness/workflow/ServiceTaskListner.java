package io.github.benxincaomu.oa.bussiness.workflow;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import jakarta.annotation.Resource;

@Service("serviceTaskListner")
public class ServiceTaskListner  implements JavaDelegate {
    @Resource
    private RestClient restClient;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        // TODO 读取属性发起请求
        
    }
    

}

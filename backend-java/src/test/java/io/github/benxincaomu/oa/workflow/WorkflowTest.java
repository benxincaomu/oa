package io.github.benxincaomu.oa.workflow;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;


import jakarta.annotation.Resource;

@SpringBootTest
public class WorkflowTest {
    @Resource
    private TaskService taskService;
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Test
    public void findByCandidateGroup() {
        // 1. 查询属于特定候选者组的任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskCandidateGroup("1")
                .includeAssignedTasks() // 可选：包含已被分配的任务
                .list();

        // 2. 提取所有唯一的流程实例 ID
        Set<String> collect = tasks.stream()
                .map(Task::getProcessInstanceId)
                .collect(Collectors.toSet());

        logger.info("Process Instance IDs: {}", collect);
    }

}

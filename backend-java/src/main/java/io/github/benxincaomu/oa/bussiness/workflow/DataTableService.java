package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.transaction.Transactional;

/**
 * 流程数据表服务
 */
@Service
public class DataTableService {

    @Resource
    private JdbcClient jdbcClient;

    @Resource
    private RedisTemplate<String, String> redisTemplate;

    @Resource
    private DataTableRepository dataTableRepository;
    @Resource
    private TransactionTemplate transactionTemplate;

    private final Logger logger = LoggerFactory.getLogger(DataTableService.class);

    @Value("${app.data-table-nums:5}")
    private Integer dataTableNums;

    private ThreadLocal<Integer> dataTableIndexLocal = new ThreadLocal<>();

    private final String flowFormTableNamePrefix = "flow_form_table_";

    private final String flowHistoryTableNamePrefix = "flow_history_table_";
    private final String flowFormAssigneeTableNamePrefix = "flow_form_assignee_table_";

    /**
     * 初始化数据表和历史表
     */
    @PostConstruct
    @Transactional
    public void initTables() {
        transactionTemplate.execute(status -> { 

            List<String> tableNames = dataTableRepository.getTableNamesLike(flowFormTableNamePrefix);
    
            int tableNums = tableNames == null ? 0 : tableNames.size();
    
            String lockKey = "initDataTableLock";
            if (tableNums < dataTableNums && Objects
                    .equals(redisTemplate.opsForValue().setIfAbsent(lockKey, "1", 30L, TimeUnit.SECONDS), Boolean.TRUE)) {
    
                int tablesToCreate = dataTableNums - tableNums;
                logger.info("当前表数量:{}", tableNums);
                logger.info("需创建表数量:{}", tablesToCreate);
                for (int i = tableNums; i < dataTableNums; i++) {
                    String flowTableName = flowFormTableNamePrefix + i;
                    logger.info("创建表:{}", flowTableName);
                    dataTableRepository.copyTable("flow_form_data_sample", flowTableName);
                    String flowHistoryTableName = flowHistoryTableNamePrefix + i;
                    dataTableRepository.copyTable("flow_history_sample", flowHistoryTableName);
    
                    String flowFormAssigneeTableName = flowFormAssigneeTableNamePrefix + i;
                    dataTableRepository.copyTable("flow_form_assignee_sample", flowFormAssigneeTableName);
    
                    logger.info("已完成创建表:{}", flowTableName);
                }
            }
            return null;
        });
    }

    public void startSelectTable() {

        List<String> tableNames = dataTableRepository.getTableNamesLike(flowFormTableNamePrefix);
        if (tableNames.size() > 0) {
            long minDataCount = 0L;
            int selectedTableIndex = 0;
            for (int i = 0; i < tableNames.size(); i++) {
                long count = dataTableRepository.count(tableNames.get(i));

                if (count < minDataCount) {
                    minDataCount = count;
                    selectedTableIndex = i;
                }
            }
            dataTableIndexLocal.set(selectedTableIndex);
        }
    }

    public void endSelectTable() {
        dataTableIndexLocal.remove();
    }

    /**
     * 选择数据表
     * 一定要先选数据表，后选历史表
     * 
     * @return
     */
    public String selectFormTable() {
        Integer index = dataTableIndexLocal.get();

        return flowFormTableNamePrefix + index;
    }

    /**
     * 选择历史表
     * 
     * @return
     */
    public String selectHistoryTable() {
        Integer index = dataTableIndexLocal.get();

        return flowHistoryTableNamePrefix + index;
    }

    /**
     * 选择任务分派表
     * 
     * @return
     */
    public String selectFlowFormAssigineeTable() {
        Integer index = dataTableIndexLocal.get();
        return flowFormAssigneeTableNamePrefix + index;
    }

}

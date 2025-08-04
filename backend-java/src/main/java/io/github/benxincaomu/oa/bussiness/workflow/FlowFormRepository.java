package io.github.benxincaomu.oa.bussiness.workflow;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FlowFormRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private ObjectMapper mapper;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 获取下一个自增ID
     * 
     * @param tableName 表名
     * @return
     */
    public Long getNextId(String tableName) {
        String sql = "select nextval(pg_get_serial_sequence(:tableName, 'id'));";
        return (Long) entityManager.createNativeQuery(sql, Long.class)
                .setParameter("tableName", tableName)
                .getSingleResult();
    }

    /**
     * 保存表单数据
     * 
     * @param flowForm    表单数据
     * @param workbenchId 工作台id
     * @param tableName   表名
     */
    public void save(FlowForm flowForm, Long workbenchId, String tableName) {
        String sql0 = "select count(id) from {0} where id=:id";
        Long count = (Long) entityManager.createNativeQuery(MessageFormat.format(sql0, tableName), Long.class)
                .setParameter("id", flowForm.getId())
                .getSingleResult();
        if (count == null || count == 0) {
            // 新增
            String sql = """
                    insert into {0} (id,publish_id,data,process_id,create_by,create_at,update_by,update_at,tenant_id,workbench_id)
                            values
                                                (:id,:publishId,:data,:processId,:createBy,:createAt,:updateBy,:updateAt,:tenantId,:workbenchId)

                            """;

            String data = null;
            try {
                data = mapper.writeValueAsString(flowForm.getData());
            } catch (JsonProcessingException e) {
                logger.error("json错误", e);
                Asserts.error();
                ;
            }

            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = JpaAuditorAware.getCurrentUserId();
            flowForm.setTenantId(JpaAuditorAware.getCurrentTenantId());
            flowForm.setCreateBy(currentUserId);
            flowForm.setUpdateBy(currentUserId);
            flowForm.setCreateAt(now);
            flowForm.setUpdateAt(now);

            entityManager.createNativeQuery(MessageFormat.format(sql, tableName), FlowForm.class)
                    .setParameter("id", flowForm.getId())
                    .setParameter("publishId", flowForm.getPublishId())
                    .setParameter("data", data)
                    .setParameter("processId", flowForm.getProcessId())
                    .setParameter("workbenchId", flowForm.getWorkbenchId())
                    .setParameter("createBy", flowForm.getCreateBy())
                    .setParameter("updateBy", flowForm.getUpdateBy())
                    .setParameter("createAt", now)
                    .setParameter("updateAt", now)
                    .setParameter("tenantId", JpaAuditorAware.getCurrentTenantId())
                    .executeUpdate();
        } else {
            // 更新
            String sql = "update {0} set process_id = :processId,data=:data, update_by = :updateBy, update_at = :updateAt where id = :id";
            String data = null;
            try {
                data = mapper.writeValueAsString(flowForm.getData());
            } catch (JsonProcessingException e) {
                logger.error("json错误", e);
                Asserts.error();
                ;
            }
            LocalDateTime now = LocalDateTime.now();
            entityManager.createNativeQuery(MessageFormat.format(sql, tableName))
                    .setParameter("id", flowForm.getId())
                    // .setParameter("publishId", flowForm.getPublishId())
                    .setParameter("data", data)
                    .setParameter("processId", flowForm.getProcessId())
                    // .setParameter("deployName", flowForm.getDeployName())
                    .setParameter("updateBy", flowForm.getUpdateBy())
                    .setParameter("updateAt", now)
                    // .setParameter("tenantId", JpaAuditorAware.getCurrentTenantId())
                    .executeUpdate();
            ;
        }

    }

    public FlowForm findOneById(Long id, String tableName) {
        String sql = "select * from {0}  where id=:id";
        FlowForm flowForm = (FlowForm) entityManager
                .createNativeQuery(MessageFormat.format(sql, tableName), FlowForm.class)
                .setParameter("id", id).getSingleResult();
        return flowForm;

    }

    /**
     * 查询我的发起
     * 
     * @param tableName  表单表名
     * @param deployName 流程定义名称
     * @param userId     用户id
     * @param currPage   当前页
     * @param pageSize   每页数量
     * @return
     */
    public Page<FlowForm> myStart(String tableName, Long workbenchId, Long userId, Integer currPage,
            Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null ? 0 : currPage - 1, pageSize == null ? 20 : pageSize,
                Sort.by("create_at desc"));
        // 查询总数
        String whereSql = "where create_by = :userId and workbench_id = :workbenchId";
        String countSql = "select count(*) from {0} " + whereSql;
        Long total = (Long) entityManager.createNativeQuery(MessageFormat.format(countSql, tableName), Long.class)
                .setParameter("userId", userId)
                .setParameter("workbenchId", workbenchId)
                .getSingleResult();
        ;
        List<FlowForm> formList = new ArrayList<>();
        if (total > page.getOffset()) {

            String sql = "select * from {0} " + whereSql +" order by create_at desc";

            formList = entityManager.createNativeQuery(MessageFormat.format(sql, tableName), FlowForm.class)
                    .setParameter("userId", userId)
                    .setParameter("workbenchId", workbenchId)
                    .setFirstResult((int) page.getOffset())
                    .setMaxResults(page.getPageSize())
                    .getResultList();
        }
        return new PageImpl<>(formList, page, total);
    }

    /**
     * 查询我的待办
     * 
     * @param tableName     表单表名
     * @param todoTableName 待办表名
     * @param workbenchId   工作流id
     * @param userId        用户id
     * @param deptId        部门id ，用于候选组筛选
     * @param starterId     发起人id 用于过滤待办
     * @param currPage      当前页
     * @param pageSize      每页数量
     * @param starterId     发起人id
     * @return
     */
    public Page<FlowForm> myToDo(String flowTableName, String todoTableName, Long workbenchId, Long userId, Long deptId,
            Long starterId, Integer currPage,
            Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null ? 0 : currPage - 1, pageSize == null ? 20 : pageSize,
                Sort.by("tt.create_at desc"));
        // 查询总数

        String whereSql = "where tt.workbench_id = :workbenchId and tt.flow_form_id = ff.id and ( tt.assignee = :userId";
        if (deptId != null) {
            whereSql += " or tt.candidate_groups @> cast(:deptId as jsonb)";
        }
        whereSql += ")";
        if (starterId != null) {
            whereSql += " and ff.create_by = :starterId";
        }
        String countSql = "select count(*) from {0} ff,{1} tt " + whereSql;
        Query countQuery = entityManager
                .createNativeQuery(MessageFormat.format(countSql, flowTableName, todoTableName), Long.class)
                .setParameter("userId", userId)
                .setParameter("workbenchId", workbenchId);
        if (deptId != null) {
            countQuery.setParameter("deptId", "[" + deptId + "]");
        }
        if (starterId != null) {
            countQuery.setParameter("starterId", starterId);
        }
        Long total = (Long) countQuery.getSingleResult();
        ;
        List<FlowForm> formList = new ArrayList<>();
        if (total > page.getOffset()) {

            String sql = "select distinct on (ff.id) ff.* from {0} ff,{1} tt " + whereSql +" order by ff.id desc,tt.create_at desc";

            Query formListQuery = entityManager
                    .createNativeQuery(MessageFormat.format(sql , flowTableName, todoTableName), FlowForm.class)
                    .setParameter("userId", userId)
                    .setParameter("workbenchId", workbenchId)
                    .setFirstResult((int) page.getOffset())
                    .setMaxResults(page.getPageSize())
                    ;
            if (deptId != null) {
                formListQuery.setParameter("deptId", "[" + deptId + "]");
            }
            if (starterId != null) {
                formListQuery.setParameter("starterId", starterId);
            }
            formList = formListQuery.getResultList();
        }
        return new PageImpl<>(formList, page, total);
    }

    /**
     * w查询我的已办
     * 
     * @param flowTableName    表单表名
     * @param historyTableName 历史表名
     * @param workbenchId      工作流id
     * @param userId           用户id
     * @param starterId        发起人id
     * @param currPage         当前页
     * @param pageSize         每页数量
     * @return
     */
    public Page<FlowForm> myDone(String flowTableName, String historyTableName, Long workbenchId, Long userId,
            Long starterId, Integer currPage, Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null ? 0 : currPage - 1, pageSize == null ? 20 : pageSize,
                Sort.by("ff.create_at desc"));
        String whereSql = "where ff.workbench_id = :workbenchId and fh.flow_form_id = ff.id and fh.create_by = :userId and ff.create_by <> :userId";

        if (starterId != null) {
            whereSql += " and ff.create_by = :starterId";
        }
        String countSql = "select  count(distinct ff.id) from {0} ff,{1} fh " + whereSql;
        Query countQuery = entityManager
                .createNativeQuery(MessageFormat.format(countSql, flowTableName, historyTableName), Long.class)
                .setParameter("userId", userId)
                .setParameter("workbenchId", workbenchId);
        if (starterId != null) {
            countQuery.setParameter("starterId", starterId);
        }
        Long total = (Long) countQuery.getSingleResult();
        List<FlowForm> formList = new ArrayList<>();
        if (total > page.getOffset()) { 
            String sql = "select distinct on (ff.id) ff.* from {0} ff,{1} fh " + whereSql +" order by ff.id desc,ff.create_at desc";
            Query formListQuery = entityManager
                    .createNativeQuery(MessageFormat.format(sql, flowTableName, historyTableName), FlowForm.class)
                    .setParameter("userId", userId)
                    .setParameter("workbenchId", workbenchId)
                    .setFirstResult((int) page.getOffset())
                    .setMaxResults(page.getPageSize())
                    ;
                    if (starterId != null) {
                        formListQuery.setParameter("starterId", starterId);
                    }
                    formList = formListQuery.getResultList();
        }
        return new PageImpl<>(formList, page, total);
    }
}

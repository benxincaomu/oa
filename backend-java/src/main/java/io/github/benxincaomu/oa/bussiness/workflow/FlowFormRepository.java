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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benxincaomu.notry.utils.Asserts;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import org.postgresql.util.PGobject;
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
     * 保存表单数据
     * 
     * @param flowForm    表单数据
     * @param workbenchId 工作台id
     * @param tableName   表名
     */
    public void save(FlowForm flowForm, Long workbenchId, String tableName) {
        if (flowForm.getId() == null) {
            // 新增
            String sql = """
                    insert into {0} (id,publish_id,data,process_id,deploy_name,create_by,create_at,update_by,update_at,tenant_id)
                            values
                                                (:id,:publishId,:data,:processId,:deployName,:createBy,:createAt,:updateBy,:updateAt,:tenantId)

                            """;

            String data = null;
            try {
                data = mapper.writeValueAsString(flowForm.getData());
            } catch (JsonProcessingException e) {
                logger.error("json错误", e);
                Asserts.error();
                ;
            }
            Long id = (Long) entityManager
                    .createNativeQuery("select nextval(pg_get_serial_sequence(:tableName, 'id'))", Long.class)
                    .setParameter("tableName", tableName)
                    .getSingleResult();
            flowForm.setId(id);
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
                    .setParameter("deployName", flowForm.getDeployName())
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
        FlowForm flowForm = (FlowForm) entityManager.createNativeQuery(MessageFormat.format(sql, tableName), FlowForm.class)
                .setParameter("id", id).getSingleResult();
        return flowForm;

    }

    /**
     * 查询我的发起
     * @param tableName 表单表名
     * @param deployName 流程定义名称
     * @param userId 用户id
     * @param currPage 当前页
     * @param pageSize 每页数量
     * @return
     */
    public Page<FlowForm> myStart(String tableName, String deployName, Long userId, Integer currPage,
            Integer pageSize) {
        PageRequest page = PageRequest.of(currPage == null ? 0 : currPage - 1, pageSize == null ? 20 : pageSize,
                Sort.by("create_at desc"));
        // 查询总数
        String whereSql = "where create_by = :userId and deploy_name = :deployName";
        String countSql = "select count(*) from {0} " + whereSql;
        Long total = (Long) entityManager.createNativeQuery(MessageFormat.format(countSql, tableName), Long.class)
                .setParameter("userId", userId)
                .setParameter("deployName", deployName)
                .getSingleResult();
        ;
        List<FlowForm> formList = new ArrayList<>();
        if (total > page.getOffset()) {

            String sql = "select * from {0} " + whereSql;

            formList = entityManager.createNativeQuery(MessageFormat.format(sql, tableName), FlowForm.class)
                    .setParameter("userId", userId)
                    .setParameter("deployName", deployName)
                    .setFirstResult((int) page.getOffset())
                    .setMaxResults(page.getPageSize())
                    .getResultList();
        }
        return new PageImpl<>(formList, page, total);
    }
}

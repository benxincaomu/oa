package io.github.benxincaomu.oa.bussiness.workflow;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@Service
public class FlowFormAssigneeRepository {
    @Resource
    private EntityManager entityManager;

    @Resource
    private ObjectMapper objectMapper ;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void save(FlowFormAssignee flowFormAssignee, String tableName) {
        String sql = """
                insert into {0}
                (flow_form_id,assignee,candidate_groups,workbench_id,actived,create_by,create_at,update_by,update_at,tenant_id)
                values
                (:flowFormId,:assignee,:candidateGroups,:workbenchId,:actived,:createBy,:createAt,:updateBy,:updateAt,:tenantId)
                """;
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = JpaAuditorAware.getCurrentUserId();
        String candidate = null;
        try {
            candidate = objectMapper.writeValueAsString(flowFormAssignee.getCandidateGroups());
        } catch (JsonProcessingException e) {
            logger.error("json错误",e);
        }
        entityManager.createNativeQuery(MessageFormat.format(sql, tableName))
                .setParameter("flowFormId", flowFormAssignee.getFlowFormId())
                .setParameter("assignee", flowFormAssignee.getAssignee())
                .setParameter("candidateGroups", candidate)
                .setParameter("workbenchId", flowFormAssignee.getWorkbenchId())
                .setParameter("actived", flowFormAssignee.getActived())
                .setParameter("createBy", currentUserId)
                .setParameter("createAt", now)
                .setParameter("updateBy", currentUserId)
                .setParameter("updateAt", now)
                .setParameter("tenantId", JpaAuditorAware.getCurrentTenantId())
                .executeUpdate();
    }

    public void delete(Long flowFormId, String tableName) {
        String sql = "delete from {0} where flow_form_id = :flowFormId";
        entityManager.createNativeQuery(MessageFormat.format(sql, tableName))
                .setParameter("flowFormId", flowFormId)
                .executeUpdate();
    }
}

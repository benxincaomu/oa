package io.github.benxincaomu.oa.bussiness.workflow;

import java.text.MessageFormat;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@Service
public class FlowFormAssigneeRepository {
    @Resource
    private EntityManager entityManager;

    public void save(FlowFormAssignee flowFormAssignee, String tableName) {
        String sql = """
                insert into {0}
                (flow_form_id,assignee,candidate_groups,workbench_id,actived,create_by,create_at,update_by,update_at,tenant_id)
                values
                (:flowFormId,:assignee,:candidateGroups,:workbenchId,:actived,:createBy,:createAt,:updateBy,:updateAt,:tenantId)
                """;
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = JpaAuditorAware.getCurrentUserId();
        entityManager.createNamedQuery(MessageFormat.format(sql, tableName))
                .setParameter("flowFormId", flowFormAssignee.getFlowFormId())
                .setParameter("assignee", flowFormAssignee.getAssignee())
                .setParameter("candidateGroups", flowFormAssignee.getCandidateGroups())
                .setParameter("workbenchId", flowFormAssignee.getWorkbenchId())
                .setParameter("actived", flowFormAssignee.getActived())
                .setParameter("createBy", currentUserId)
                .setParameter("createAt", now)
                .setParameter("updateBy", currentUserId)
                .setParameter("updateAt", now)
                .setParameter("tenantId", JpaAuditorAware.getCurrentTenantId())
                .executeUpdate();
    }
}

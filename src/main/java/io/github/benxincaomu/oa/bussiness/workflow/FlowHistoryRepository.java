package io.github.benxincaomu.oa.bussiness.workflow;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.base.security.KeycloakDirectoryService;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@Service
public class FlowHistoryRepository {

    @Resource
    private EntityManager entityManager;

    @Resource
    private KeycloakDirectoryService directoryService;

    /**
     * 保存流程历史
     *
     * <p>
     * The operator display name is resolved from Keycloak and stored as a
     * historical snapshot, so audit rows stay readable after a Keycloak user is
     * renamed or disabled.
     * </p>
     *
     * @param flowHistory 流程历史
     * @param tableName   表名
     */
    public void save(FlowHistory flowHistory, String tableName) {
        String sql = """
                insert into {0} (flow_form_id,approval_opinion,flow_name,create_by,create_at,update_by,update_at,tenant_id,operator_name,node_name)
                values          (:flowFormId,:approvalOpinion,:flowName,:createBy,:createAt,:updateBy,:updateAt,:tenantId,:operatorName,:nodeName)
                """;
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = JpaAuditorAware.getCurrentUserId();
        String operatorName = flowHistory.getOperatorName();
        if (!StringUtils.hasText(operatorName)) {
            operatorName = directoryService.displayNameForUser(currentUserId);
        }
        // operator_name 列是 varchar(20)，Keycloak 姓名可能更长，写入前截断避免审批失败
        if (operatorName != null && operatorName.length() > 20) {
            operatorName = operatorName.substring(0, 20);
        }
        entityManager.createNativeQuery(MessageFormat.format(sql, tableName))
                .setParameter("flowFormId", flowHistory.getFlowFormId())
                .setParameter("approvalOpinion", flowHistory.getApprovalOpinion())
                .setParameter("flowName", flowHistory.getFlowName())
                .setParameter("operatorName", operatorName)
                .setParameter("createBy", currentUserId)
                .setParameter("createAt", now)
                .setParameter("updateBy", currentUserId)
                .setParameter("updateAt", now)
                .setParameter("tenantId", JpaAuditorAware.getCurrentTenantId())
                .setParameter("nodeName", flowHistory.getNodeName())
                .executeUpdate();
        ;
    }

    public List<FlowHistory> findAll(Long flowFormId, String tableName) {
        String sql = "select * from {0} where flow_form_id = :flowFormId ";
        return entityManager.createNativeQuery(MessageFormat.format(sql, tableName), FlowHistory.class)
                .setParameter("flowFormId", flowFormId)
                .getResultList();

    }

}

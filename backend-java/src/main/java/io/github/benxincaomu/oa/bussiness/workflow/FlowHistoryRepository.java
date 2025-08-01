package io.github.benxincaomu.oa.bussiness.workflow;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.benxincaomu.oa.base.entity.JpaAuditorAware;
import io.github.benxincaomu.oa.bussiness.user.User;
import io.github.benxincaomu.oa.bussiness.user.UserRepository;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

@Service
public class FlowHistoryRepository {

    @Resource
    private EntityManager entityManager;

    @Resource
    private UserRepository userRepository;

    /**
     * 保存流程历史
     * @param flowHistory 流程历史
     * @param tableName 表名
     */
    public void save(FlowHistory flowHistory, String tableName) {
        String sql = """
                insert into {0} (flow_form_id,approval_opinion,flow_name,create_by,create_at,update_by,update_at,tenant_id,operator_name)
                values          (:flowFormId,:approvalOpinion,:flowName,:createBy,:createAt,:updateBy,:updateAt,:tenantId,:operatorName)
                """;
        LocalDateTime now = LocalDateTime.now();
        Long currentUserId = JpaAuditorAware.getCurrentUserId();
        User user = userRepository.findById(currentUserId).orElse(new User());
        entityManager.createNativeQuery(MessageFormat.format(sql, tableName))
                .setParameter("flowFormId", flowHistory.getFlowFormId())
                .setParameter("approvalOpinion", flowHistory.getApprovalOpinion())
                .setParameter("flowName", flowHistory.getFlowName())
                .setParameter("operatorName", user.getName())
                .setParameter("createBy", currentUserId)
                .setParameter("createAt", now)
                .setParameter("updateBy", currentUserId)
                .setParameter("updateAt", now)
                .setParameter("tenantId", JpaAuditorAware.getCurrentUserId())
                .executeUpdate()
                ;

    }

    public List<FlowHistory> findAll(Long flowFormId, String tableName) {
        String sql = "select * from {0} where flow_form_id = :flowFormId ";
        return entityManager.createNativeQuery(MessageFormat.format(sql, tableName), FlowHistory.class)
                .setParameter("flowFormId", flowFormId)
                .getResultList();

    }

}

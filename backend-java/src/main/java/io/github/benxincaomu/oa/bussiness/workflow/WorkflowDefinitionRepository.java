package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;


@Repository
public interface WorkflowDefinitionRepository extends JpaRepository<WorkflowDefinition,Long> {

    Optional<WorkflowDefinition> findOneByWorkbenchId(Long workbenchId);

    boolean existsByWorkbenchId(Long workbenchId);

    @Query("select id from WorkflowDefinition where workbenchId = ?1")
    Long findIdByWorkbenchId(Long workbenchId);

    @Modifying
    @Query("update WorkflowDefinition set flowDefinition = ?1 where workbenchId = ?2")
    void updateByWorkbenchId(WorkflowDefinition workflowDefinition);
}

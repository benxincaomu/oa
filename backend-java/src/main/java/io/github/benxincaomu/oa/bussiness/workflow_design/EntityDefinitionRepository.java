package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;


@Repository
public interface EntityDefinitionRepository  extends JpaRepository<EntityDefinition, Long> {

    List<EntityDefinition> findOneByIdAndTenantId(@Param("id")Long id, @Param("tenantId")Long tenantId);

    Optional<EntityDefinition> findOneByWorkbenchId(Long workbenchId);

}

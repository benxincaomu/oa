package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface WorkbenchRepository extends JpaRepository<Workbench, Long> {

    Workbench findOneByIdAndTenantId(Long id, long l);

}

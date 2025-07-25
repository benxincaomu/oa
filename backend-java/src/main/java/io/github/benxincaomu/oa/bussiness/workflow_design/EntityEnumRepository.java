package io.github.benxincaomu.oa.bussiness.workflow_design;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface EntityEnumRepository extends JpaRepository<EntityEnum,Long> {

}

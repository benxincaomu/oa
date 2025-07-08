package io.github.benxincaomu.oa.bussiness.workflow;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestTableRepository extends JpaRepository<TestTable,Long> {

}

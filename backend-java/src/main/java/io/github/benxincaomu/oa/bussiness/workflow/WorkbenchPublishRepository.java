package io.github.benxincaomu.oa.bussiness.workflow;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkbenchPublishRepository extends JpaRepository<WorkbenchPublish, Long> {

    @Query("select w from WorkbenchPublish w where w.workbenchId = ?1")
    List<WorkbenchPublish> listByWorkbenchId(Long workbenchId);

    @Query("select max(w.version) from WorkbenchPublish w where w.workbenchId = ?1")
    Long findMaxVersionByWorkbenchId(Long workbenchId);

    @Query("SELECT w FROM WorkbenchPublish w WHERE w.workbenchId = ?1 AND w.version = (SELECT MAX(w2.version) FROM WorkbenchPublish w2 WHERE w2.workbenchId = ?1)")
    Optional<WorkbenchPublish> findLatestByWorkbenchId(Long workbenchId);

}

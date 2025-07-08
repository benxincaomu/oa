package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;

@Repository
public interface DepartmentRepository extends JpaRepository<Department,Long> {

    boolean existsByName(String name);

    @Query("select d.id,d.name,d.code,d.parentId from Department d where CASE WHEN :parentId is NULL THEN d.parentId is null ELSE d.parentId = :parentId END")
    List<DeptVo> findAllByParentId(@Param("parentId")Long parentId);

}

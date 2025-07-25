package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo;
import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

  boolean existsByName(String name);

  @Query("select new io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo(d.id, d.name, d.parentId,d.code) from Department d where CASE WHEN :parentId is NULL THEN d.parentId is null ELSE d.parentId = :parentId END")
  List<DeptVo> findAllByParentId(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);

  @Query("select d from Department d where d.parentId is null")
  List<Department> findAllDeptAsTree();

  @Query("select new io.github.benxincaomu.oa.bussiness.organization.vo.DeptVo(d.id, d.name, d.parentId,d.code)   from Department d")
  List<DeptVo> listAll();

  @Modifying
  @Query("update Department set leaderUserId = :#{#dept.leaderUserId} where id=:#{#dept.id}")
  void updateLeader(@Param("dept") Department department);

  /*
   * @Query("select d.id as id,d.name as name,d.parentId as parentId from Department d "
   * +
   * "where CASE WHEN :parentId is NULL THEN d.parentId is null ELSE d.parentId = :parentId END"
   * )
   * List<DeptVo> findByParentId(@Param("parentId") Long parentId);
   */

}

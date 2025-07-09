package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;

@Repository
public interface PermissionRepository extends JpaRepository<Permission,Long>{

    @Query("select p from Permission p,RolePermission rp where rp.roleId = :roleId and rp.permissionId = p.id")
    List<Permission> findByRoleId(Long roleId);

    @Query("select p.id as id,p.name as name,p.parentId as parentId from Permission p where p.parentId = :parentId ")
    List<PermissionIdName> findByParentMenuId(Long parentId);

    @Query("select p.id as id,p.name as name,p.parentId as parentId from Permission p where p.parentId is null")
    List<PermissionIdName> findTopMenu();

    @Query("select p.id as id,p.name as name,p.parentId as parentId from Permission p where p.type = :type")
    List<PermissionIdName> findIdAndNameByType(@Param("type")Integer type);

} 
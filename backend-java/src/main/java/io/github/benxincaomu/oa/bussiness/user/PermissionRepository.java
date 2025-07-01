package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.benxincaomu.oa.bussiness.user.vo.ParentPermission;

// @Repository
public interface PermissionRepository extends JpaRepository<Permission,Long>{

    @Query("select p from Permission p,RolePermission rp where rp.roleId = :roleId and rp.permissionId = p.id")
    List<Permission> findByRoleId(Long roleId);

    List<Permission> findByParentId(Long parentId);

    @Query("select p from Permission p where p.parentId is null")
    List<Permission> findTop();

    @Query("select p.id as id,p.name as name from Permission p where p.type = :type")
    List<ParentPermission> findIdAndNameByType(@Param("type")Integer type);
} 
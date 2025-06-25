package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

// @Repository
public interface PermissionRepository extends JpaRepository<Permission,Long>{

    @Query("select p from Permission p,RolePermission rp where rp.roleId = :roleId and rp.permissionId = p.id")
    List<Permission> findByRoleId(Long roleId);
} 
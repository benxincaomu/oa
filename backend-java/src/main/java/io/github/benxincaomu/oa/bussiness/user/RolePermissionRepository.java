package io.github.benxincaomu.oa.bussiness.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.bussiness.user.vo.PermissionIdName;

import java.util.List;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("select rp.permissionId from RolePermission rp where rp.roleId = :roleId")
    List<Long> findPermissionIdByRoleId(Long roleId);

    /**
     * 根据角色id和父级菜单id查询菜单
     * 
     * @param roleId
     * @param parentId
     * @return
     */
    @Query("select p.id as id,p.name as name,p.parentId as parentId,p.type type,p.value as value from Permission p,RolePermission rp where rp.roleId = :roleId and rp.permissionId = p.id "
            +
            "and CASE WHEN :parentId IS NULL THEN p.parentId IS NULL ELSE p.parentId = :parentId END")
    List<PermissionIdName> findPermissionIdNameByRoleId(@Param("roleId") Long roleId, @Param("parentId") Long parentId);

    void deleteByRoleId(Long roleId);

    /**
     * 根据角色id查询权限树
     * jpa无法处理Integer和Long的转换，暂时弃用 
     * @see findPermissionIdNameByRoleId
     * @deprecated jpa无法处理Integer和Long的转换，暂时弃用 
     * @param roleId
     * @return
     */
    @Query(value = "WITH RECURSIVE tree AS ( \n" + 
                "    SELECT p.id as id, p.parent_id as parent_id, p.name as name,p.type as type \n" +
                "    FROM permission p,role_permission rp \n" + 
                "    WHERE  \n" + 
                "    rp.role_id = :roleId and rp.permission_id = p.id and p.parent_id IS NULL \n" + 
                "    UNION ALL \n" + 
                "    SELECT p.id as id, p.parent_id as parent_id, p.name as name,p.type as type \n" +
                "    FROM permission p,role_permission rp,tree t \n" + 
                "    WHERE  \n" + 
                "    rp.role_id = :roleId and rp.permission_id = p.id and p.parent_id = t.id\n" + 
                ")\n" + 
                "SELECT id, name, type, parent_id \n" + 
                "FROM tree",
    nativeQuery=true)
    List<PermissionIdName> findTreeByRoleId(Long roleId);



}

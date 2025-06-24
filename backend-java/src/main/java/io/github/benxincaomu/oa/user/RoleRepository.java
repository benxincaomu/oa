package io.github.benxincaomu.oa.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
// @Repository
public interface RoleRepository extends JpaRepository<Role,Long>{

    @Query("select r from Role r,UserRole ur where r.id = ur.roleId and ur.userId = :userId")
    Role findByUserId(@Param("userId") Long userId);
}
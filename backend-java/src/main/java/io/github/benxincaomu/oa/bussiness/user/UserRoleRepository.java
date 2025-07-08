package io.github.benxincaomu.oa.bussiness.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    @Query("select r.roleId from UserRole r where r.userId = :userId")
    Long getRoleIdByUserId(@Param("userId") Long userId);

    void deleteByUserId(Long userId);

}

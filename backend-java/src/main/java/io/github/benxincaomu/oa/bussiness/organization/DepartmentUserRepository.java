package io.github.benxincaomu.oa.bussiness.organization;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.bussiness.user.User;

@Repository
public interface DepartmentUserRepository extends JpaRepository<DepartmentUser, Long> {

    @Query("select u from User u,DepartmentUser du where u.id = du.userId and du.departmentId = :deptId")
    List<User> findUsersByDeptId(Long deptId);

    @Query("select du.departmentId from DepartmentUser du where du.userId = :userId")
    Optional<Long> findDeptIdByUserId(Long userId);

    void deleteByUserId(Long userId);
}

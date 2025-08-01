package io.github.benxincaomu.oa.bussiness.user;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.benxincaomu.oa.bussiness.user.vo.UserIdNameVo;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public User findByUserName(String userName);

    public boolean existsByUserName(String userName);

    public boolean existsByEmail(String email);

    // @EntityGraph(value = "user.withoutSalt", type =
    // EntityGraph.EntityGraphType.FETCH)
    // Page<User> findByExample(Example<User> example, Pageable pageable);

    @Modifying
    @Query("update User set enable = :enable where id = :id")
    public void updateEnable(@Param("id") Long id, @Param("enable") boolean enable);

    @Modifying
    @Query("update User set password = :password,enable = true where id = :id")
    public void updatePassword(@Param("id") Long id, @Param("password") String password);

    @Query("""
            select u.id as id, CONCAT(u.name,'-',d.name )as name
            from User u,
            io.github.benxincaomu.oa.bussiness.organization.Department d,
            io.github.benxincaomu.oa.bussiness.organization.DepartmentUser du
            where u.name like concat('%',:name,'%') and u.id = du.userId and du.departmentId = d.id and u.tenantId = :tenantId
                """)
    public List<UserIdNameVo> findUsersByNameLike(@Param("name")String name,@Param("tenantId")Long tenantId);

    /**
     * 根据用户id获取上级领导id
     * @param userId 用户id
     * @return 上级领导id
     */
    @Query("""
            select d.leaderUserId from 
            io.github.benxincaomu.oa.bussiness.organization.DepartmentUser du,
            io.github.benxincaomu.oa.bussiness.organization.Department d
            where du.userId =:userId and du.departmentId = d.id 

            """
    )
    public Long findLeaderId(@Param("userId")Long userId);

    /**
     * 用于选择用户
     * @param tenantId  租户id
     * @return
     */
      @Query("""
            select u.id as id, CONCAT(u.name,'-',d.name )as name
            from User u,
            io.github.benxincaomu.oa.bussiness.organization.Department d,
            io.github.benxincaomu.oa.bussiness.organization.DepartmentUser du
            where u.id = du.userId and du.departmentId = d.id and u.tenantId = :tenantId
                """)
    public List<UserIdNameVo> getAllUsers(@Param("tenantId")Long tenantId);

}

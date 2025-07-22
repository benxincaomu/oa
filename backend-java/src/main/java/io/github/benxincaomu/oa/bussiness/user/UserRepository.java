package io.github.benxincaomu.oa.bussiness.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    public User findByUserName(String userName);

    public boolean existsByUserName(String userName);

    public boolean existsByEmail(String email);

    // @EntityGraph(value = "user.withoutSalt", type = EntityGraph.EntityGraphType.FETCH)
    // Page<User> findByExample(Example<User> example, Pageable pageable);

    @Modifying
    @Query("update User set enable = :enable where id = :id")
    public void updateEnable(@Param("id") Long id, @Param("enable") boolean enable);

    @Modifying
    @Query("update User set password = :password,enable = true where id = :id")
    public void updatePassword(@Param("id") Long id, @Param("password") String password);

    

}

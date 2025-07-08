package io.github.benxincaomu.oa.bussiness.user;

import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    public User findByUserName(String userName);

    public boolean existsByUserName(String userName);

    // @EntityGraph(value = "user.withoutSalt", type = EntityGraph.EntityGraphType.FETCH)
    // Page<User> findByExample(Example<User> example, Pageable pageable);

    @Modifying
    @Query("update User set enable = :enable where id = :id")
    public void updateEnable(@Param("id") Long id, @Param("enable") boolean enable);

}

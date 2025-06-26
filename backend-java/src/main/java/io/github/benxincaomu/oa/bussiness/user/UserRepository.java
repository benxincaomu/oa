package io.github.benxincaomu.oa.bussiness.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface UserRepository extends JpaRepository<User, Long>{
    public User findByUserName(String userName);

    public boolean existsByUserName(String userName);
}

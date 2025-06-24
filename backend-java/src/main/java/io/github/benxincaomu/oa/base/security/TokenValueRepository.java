package io.github.benxincaomu.oa.base.security;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface TokenValueRepository extends CrudRepository<TokenValue,String> {

}

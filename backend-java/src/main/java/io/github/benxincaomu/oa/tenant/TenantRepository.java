package io.github.benxincaomu.oa.tenant;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

// @Repository
public interface TenantRepository extends JpaRepository<Tenant, Long>{


}

package io.github.benxincaomu.oa.base.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.util.CollectionUtils;

import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;

public abstract class BaseService {

    @Resource
    private EntityManager entityManager;

    public <T extends BaseEntity>void saveAll(List<T> entities){
        if(!CollectionUtils.isEmpty(entities)){
            LocalDateTime now = LocalDateTime.now();
            Long currentUserId = JpaAuditorAware.getCurrentUserId();
            Long tenantId = JpaAuditorAware.getCurrentTenantId();
            for(T entity : entities){
                /* entity.setCreateAt(now);
                entity.setUpdateAt(now);
                entity.setCreateBy(currentUserId);
                entity.setUpdateBy(currentUserId);
                entity.setTenantId(tenantId); */
                entityManager.persist(entity);
            }
            entityManager.flush();
            entityManager.clear();
        }

    }

}

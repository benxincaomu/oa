package io.github.benxincaomu.oa.bussiness.tenant;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Service
@Order(Integer.MAX_VALUE)
public class TenantService {

    @Resource
    private TenantRepository tenantRepository;




}

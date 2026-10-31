package io.github.benxincaomu.oa.base.init;

import java.util.Comparator;

import org.springframework.stereotype.Component;

import io.github.benxincaomu.oa.bussiness.tenant.Tenant;
import io.github.benxincaomu.oa.bussiness.tenant.TenantRepository;

/**
 * Provides the single local tenant used for numeric IDs, resource keys and audit
 * columns.
 *
 * <p>
 * Tenant membership is no longer encoded in Keycloak group attributes: all
 * Keycloak groups belong to this default tenant and the root group is whatever the
 * Keycloak administrator created. The tenant row is created automatically on first
 * use when the database does not contain one.
 * </p>
 */
@Component
public class OaTenantProvider {
    private final TenantRepository tenantRepository;
    private volatile Long tenantId;

    public OaTenantProvider(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public synchronized Long getTenantId() {
        Long cached = tenantId;
        if (cached != null) {
            return cached;
        }
        Tenant tenant = tenantRepository.findAll().stream()
                .min(Comparator.comparing(Tenant::getId))
                .orElseGet(this::createDefaultTenant);
        tenantId = tenant.getId();
        return tenantId;
    }

    private Tenant createDefaultTenant() {
        Tenant tenant = new Tenant();
        tenant.setTenantName("default");
        tenant.setEnable(true);
        return tenantRepository.save(tenant);
    }
}

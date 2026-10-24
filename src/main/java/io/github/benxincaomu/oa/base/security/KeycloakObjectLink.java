package io.github.benxincaomu.oa.base.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "keycloak_object_links", uniqueConstraints = {
        @UniqueConstraint(name = "keycloak_link_external_unique", columnNames = { "object_type", "external_id" }),
        @UniqueConstraint(name = "keycloak_link_local_unique", columnNames = { "object_type", "local_id" })
})
public class KeycloakObjectLink {
    public static final String USER = "USER";
    public static final String DEPARTMENT = "DEPARTMENT";
    public static final String ROLE = "ROLE";
    public static final String PERMISSION = "PERMISSION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_type", nullable = false, length = 32)
    private String objectType;

    @Column(name = "external_id", nullable = false, length = 128)
    private String externalId;

    @Column(name = "local_id", nullable = false)
    private Long localId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public Long getLocalId() {
        return localId;
    }

    public void setLocalId(Long localId) {
        this.localId = localId;
    }
}

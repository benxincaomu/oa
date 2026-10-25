package io.github.benxincaomu.oa.base.security;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface KeycloakObjectLinkRepository extends JpaRepository<KeycloakObjectLink, Long> {
    Optional<KeycloakObjectLink> findByObjectTypeAndExternalId(String objectType, String externalId);

    Optional<KeycloakObjectLink> findByObjectTypeAndLocalId(String objectType, Long localId);

    List<KeycloakObjectLink> findByObjectTypeAndLocalIdIn(String objectType, Collection<Long> localIds);
}

package com.deepfine.inventorysystem.infrastructure.persistence.tenant;

import com.deepfine.inventorysystem.domain.tenant.Tenant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantJpaRepository extends JpaRepository<Tenant, Long> {

    Optional<Tenant> findByCode(String code);
}

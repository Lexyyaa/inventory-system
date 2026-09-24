package com.deepfine.inventorysystem.infrastructure.persistence.tenant;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.tenant.Tenant;
import com.deepfine.inventorysystem.domain.tenant.TenantRepository;
import com.deepfine.inventorysystem.domain.tenant.exception.TenantException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantJpaRepository tenantJpaRepository;

    @Override
    public Tenant getByCode(String code) {
        return tenantJpaRepository.findByCode(code).orElseThrow(() -> new TenantException(ErrorCode.INVALID_TENANT));
    }
}

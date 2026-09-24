package com.deepfine.inventorysystem.domain.tenant;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.tenant.exception.TenantException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;

    public Long getId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new TenantException(ErrorCode.INVALID_TENANT);
        }
        return tenantRepository.getByCode(tenantCode).getId();
    }
}

package com.deepfine.inventorysystem.application.tenant;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.tenant.TenantRepository;
import com.deepfine.inventorysystem.domain.tenant.exception.TenantException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantApplicationService {

    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public Long getTenantId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new TenantException(ErrorCode.INVALID_TENANT);
        }
        return tenantRepository.getByCode(tenantCode).getId();
    }
}

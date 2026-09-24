package com.deepfine.inventorysystem.application.tenant;

import com.deepfine.inventorysystem.domain.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TenantApplicationService {

    private final TenantService tenantService;

    @Transactional(readOnly = true)
    public Long getTenantId(String tenantCode) {
        return tenantService.getId(tenantCode);
    }
}

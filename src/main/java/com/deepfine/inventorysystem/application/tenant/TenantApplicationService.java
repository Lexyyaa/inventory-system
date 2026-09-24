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

    /**
     * X-Tenant-Id 헤더 값(업체 코드)으로 등록된 업체의 id를 돌려준다.
     * 값이 없거나 비어 있거나 공백뿐이거나 등록되지 않은 코드면 INVALID_TENANT다. 길이 · 형식은 따로 검사하지 않는다.
     */
    @Transactional(readOnly = true)
    public Long getTenantId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new TenantException(ErrorCode.INVALID_TENANT);
        }
        return tenantRepository.getByCode(tenantCode).getId();
    }
}

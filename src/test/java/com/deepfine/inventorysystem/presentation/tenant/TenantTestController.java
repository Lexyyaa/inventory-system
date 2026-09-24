package com.deepfine.inventorysystem.presentation.tenant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * TC-1-01 · TC-1-02 전용 엔드포인트. 입고 · 조회 · 출고 API가 생기기 전에 업체 확인 인터셉터를 검증한다.
 *
 * <p>테스트 클래스 안에 중첩하면 컴포넌트 스캔에서 빠져 등록되지 않는다.
 * 그러면 인터셉터만 동작해 실패 케이스가 거짓으로 통과하므로 최상위 클래스로 둔다.
 */
@RestController
public class TenantTestController {

    @GetMapping("/api/v1/test/tenant")
    public TenantTestResponse tenant(
            @TenantId Long tenantId, @RequestHeader(TenantInterceptor.TENANT_HEADER) String tenantCode) {
        return new TenantTestResponse(tenantId, tenantCode);
    }

    public record TenantTestResponse(Long tenantId, String tenantCode) {}
}

package com.deepfine.inventorysystem.presentation.tenant;

import com.deepfine.inventorysystem.application.tenant.TenantApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * {@code /api/v1/**} 요청의 X-Tenant-Id를 요청 본문 해석보다 먼저 확인한다 (04 §2 "오류 판정 순서"의 1번).
 *
 * <p>확인 실패는 TenantApplicationService가 INVALID_TENANT로 던지고 GlobalExceptionHandler가 응답으로 바꾼다.
 * 확인된 업체 id는 request attribute에 넣고, 컨트롤러는 {@link TenantId}로 꺼낸다.
 */
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    static final String TENANT_ID_ATTRIBUTE = TenantInterceptor.class.getName() + ".tenantId";

    private final TenantApplicationService tenantApplicationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long tenantId = tenantApplicationService.getTenantId(request.getHeader(TENANT_HEADER));
        request.setAttribute(TENANT_ID_ATTRIBUTE, tenantId);
        return true;
    }
}

package com.deepfine.inventorysystem.presentation.interceptor;

import com.deepfine.inventorysystem.application.tenant.TenantApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 업체 확인 <br>
 * - 요청 본문을 해석하기 전에 X-Tenant-Id를 확인하려고 인터셉터에서 한다 <br>
 * - 확인된 업체 id는 request attribute(TENANT_ID)로 컨트롤러에 넘긴다 <br>
 */
@Component
@RequiredArgsConstructor
public class TenantInterceptor implements HandlerInterceptor {

    public static final String TENANT_HEADER = "X-Tenant-Id";
    public static final String TENANT_ID = "tenantId";

    private final TenantApplicationService tenantApplicationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long tenantId = tenantApplicationService.getTenantId(request.getHeader(TENANT_HEADER));
        request.setAttribute(TENANT_ID, tenantId);
        return true;
    }
}

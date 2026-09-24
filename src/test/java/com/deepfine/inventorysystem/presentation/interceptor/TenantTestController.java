package com.deepfine.inventorysystem.presentation.interceptor;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_ID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantTestController {

    @GetMapping("/test/tenant")
    public TenantTestResponse tenant(
            @RequestAttribute(TENANT_ID) Long tenantId, @RequestHeader(TENANT_HEADER) String tenantCode) {
        return new TenantTestResponse(tenantId, tenantCode);
    }

    public record TenantTestResponse(Long tenantId, String tenantCode) {}
}

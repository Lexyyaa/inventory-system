package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_ID;

import com.deepfine.inventorysystem.application.inventory.InventoryApplicationService;
import com.deepfine.inventorysystem.application.inventory.InventoryCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 API. 업체 확인은 TenantInterceptor가 본문 해석보다 먼저 끝낸다.
 * 매핑에 consumes · produces를 두지 않는다 (Content-Type 오류가 업체 확인보다 먼저 판정되지 않게).
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController implements InventoryApiDocs {

    private final InventoryApplicationService inventoryApplicationService;

    @Override
    @PostMapping("/inbound")
    public InventoryResponse.Inbound inbound(
            @RequestAttribute(TENANT_ID) Long tenantId, @RequestBody @Valid InventoryRequest.Inbound request) {
        return InventoryResponse.Inbound.from(inventoryApplicationService.inbound(
                tenantId,
                InventoryCommand.Inbound.of(request.productCode(), request.productName(), request.quantity())));
    }
}

package com.deepfine.inventorysystem.application.inventory;

import com.deepfine.inventorysystem.domain.inventory.Inventory;
import com.deepfine.inventorysystem.domain.inventory.InventoryService;
import com.deepfine.inventorysystem.domain.inventory.InventoryState;
import com.deepfine.inventorysystem.domain.product.Product;
import com.deepfine.inventorysystem.domain.product.ProductService;
import com.deepfine.inventorysystem.support.properties.InventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final ProductService productService;
    private final InventoryService inventoryService;
    private final InventoryProperties inventoryProperties;

    /**
     * 입고 <br>
     * - 상품 생성, 상품명 검증, 재고 증가를 한 트랜잭션으로 처리 <br>
     * - 어느 단계든 실패하면 모두 롤백 <br>
     */
    @Transactional
    public InventoryInfo.Inbound inbound(InventoryCommand.Inbound command) {
        Inventory.validateInboundQuantity(command.quantity(), inventoryProperties.maxQuantity());
        Product product = productService.getOrCreate(command.tenantId(), command.productCode(), command.productName());
        InventoryState changed = inventoryService.increase(product.getId(), command.quantity());
        return InventoryInfo.Inbound.of(product, changed);
    }

    @Transactional
    public InventoryInfo.Outbound outbound(InventoryCommand.Outbound command) {
        Inventory.validateOutboundQuantity(command.quantity(), inventoryProperties.maxQuantity());
        Product product = productService.get(command.tenantId(), command.productCode());
        InventoryState changed = inventoryService.decrease(product.getId(), command.quantity());
        return InventoryInfo.Outbound.of(product, changed);
    }

    @Transactional(readOnly = true)
    public InventoryInfo.CurrentStock getCurrentStock(InventoryCommand.CurrentStock command) {
        Product product = productService.get(command.tenantId(), command.productCode());
        Inventory inventory = inventoryService.get(product.getId());
        return InventoryInfo.CurrentStock.of(product, inventory);
    }
}

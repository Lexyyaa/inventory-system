package com.deepfine.inventorysystem.application.inventory;

import com.deepfine.inventorysystem.domain.inventory.Inventory;
import com.deepfine.inventorysystem.domain.inventory.InventoryRepository;
import com.deepfine.inventorysystem.domain.inventory.InventorySnapshot;
import com.deepfine.inventorysystem.domain.product.Product;
import com.deepfine.inventorysystem.domain.product.ProductRepository;
import com.deepfine.inventorysystem.support.properties.InventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryApplicationService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryProperties inventoryProperties;

    /**
     * 입고한다 (03 §8 · §14). 상품 생성 시도 → 상품명 검증 → 재고 증가를 한 트랜잭션에서 한다.
     * 어느 단계에서 실패해도 상품과 재고를 남기지 않는다.
     *
     * <p>수량 범위는 상품을 만들기 전에 검사한다 (04 §2의 4번). 신규 · 기존 상품 모두 생성 시도 뒤 다시 읽어 같은 경로로 처리한다.
     */
    @Transactional
    public InventoryInfo.Inbound inbound(Long tenantId, InventoryCommand.Inbound command) {
        Inventory.validateInboundQuantity(command.quantity(), inventoryProperties.maxQuantity());

        productRepository.createIfAbsent(Product.create(tenantId, command.productCode(), command.productName()));
        Product product = productRepository.getByTenantIdAndProductCode(tenantId, command.productCode());
        product.validateName(command.productName());

        InventorySnapshot snapshot = inventoryRepository.increase(product.getId(), command.quantity());
        return InventoryInfo.Inbound.of(product, snapshot);
    }
}

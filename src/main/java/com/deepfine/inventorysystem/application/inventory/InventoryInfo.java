package com.deepfine.inventorysystem.application.inventory;

import com.deepfine.inventorysystem.domain.inventory.InventorySnapshot;
import com.deepfine.inventorysystem.domain.product.Product;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryInfo {

    /**
     * 입고 결과. 상품코드 · 상품명은 저장된 상품 값, 수량 · 변경 시각은 재고 증가 SQL의 {@code RETURNING} 값이다.
     */
    public record Inbound(String productCode, String productName, long quantity, OffsetDateTime updatedAt) {

        public static Inbound of(Product product, InventorySnapshot snapshot) {
            return new Inbound(product.getProductCode(), product.getName(), snapshot.quantity(), snapshot.updatedAt());
        }
    }
}

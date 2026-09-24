package com.deepfine.inventorysystem.application.inventory;

import com.deepfine.inventorysystem.domain.inventory.Inventory;
import com.deepfine.inventorysystem.domain.inventory.InventoryState;
import com.deepfine.inventorysystem.domain.product.Product;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryInfo {

    public record Inbound(String productCode, String productName, long quantity, Instant updatedAt) {

        public static Inbound of(Product product, InventoryState state) {
            return new Inbound(product.getProductCode(), product.getName(), state.quantity(), state.updatedAt());
        }
    }

    public record Outbound(String productCode, String productName, long quantity, Instant updatedAt) {

        public static Outbound of(Product product, InventoryState state) {
            return new Outbound(product.getProductCode(), product.getName(), state.quantity(), state.updatedAt());
        }
    }

    public record CurrentStock(String productCode, String productName, long quantity, Instant updatedAt) {

        public static CurrentStock of(Product product, Inventory inventory) {
            return new CurrentStock(
                    product.getProductCode(), product.getName(), inventory.getQuantity(), inventory.getUpdatedAt());
        }
    }
}

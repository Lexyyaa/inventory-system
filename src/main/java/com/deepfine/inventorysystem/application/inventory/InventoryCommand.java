package com.deepfine.inventorysystem.application.inventory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryCommand {

    public record Inbound(Long tenantId, String productCode, String productName, long quantity) {}

    public record Outbound(Long tenantId, String productCode, long quantity) {}

    public record CurrentStock(Long tenantId, String productCode) {}
}

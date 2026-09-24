package com.deepfine.inventorysystem.domain.inventory;

import java.util.Optional;

public interface InventoryRepository {

    InventoryState increase(Long productId, long quantity);

    Optional<Inventory> findByProductId(Long productId);
}

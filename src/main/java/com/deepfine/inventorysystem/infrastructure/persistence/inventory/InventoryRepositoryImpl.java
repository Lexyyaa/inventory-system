package com.deepfine.inventorysystem.infrastructure.persistence.inventory;

import com.deepfine.inventorysystem.domain.inventory.InventoryRepository;
import com.deepfine.inventorysystem.domain.inventory.InventoryState;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;

    @Override
    public InventoryState increase(Long productId, long quantity) {
        return inventoryJpaRepository.upsertIncrease(productId, quantity);
    }
}

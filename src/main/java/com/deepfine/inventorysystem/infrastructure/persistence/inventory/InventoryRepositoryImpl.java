package com.deepfine.inventorysystem.infrastructure.persistence.inventory;

import com.deepfine.inventorysystem.domain.inventory.Inventory;
import com.deepfine.inventorysystem.domain.inventory.InventoryRepository;
import com.deepfine.inventorysystem.domain.inventory.InventoryState;
import java.util.Optional;
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

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return inventoryJpaRepository.findById(productId);
    }
}

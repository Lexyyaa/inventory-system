package com.deepfine.inventorysystem.infrastructure.persistence.inventory;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.inventory.Inventory;
import com.deepfine.inventorysystem.domain.inventory.InventoryRepository;
import com.deepfine.inventorysystem.domain.inventory.InventoryState;
import com.deepfine.inventorysystem.domain.inventory.exception.InventoryException;
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
    public InventoryState decrease(Long productId, long quantity) {
        return inventoryJpaRepository
                .decreaseIfSufficient(productId, quantity)
                .orElseThrow(() -> new InventoryException(ErrorCode.INSUFFICIENT_STOCK));
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return inventoryJpaRepository.findById(productId);
    }
}

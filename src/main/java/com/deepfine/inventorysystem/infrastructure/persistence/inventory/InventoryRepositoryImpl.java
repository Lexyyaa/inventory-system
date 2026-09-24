package com.deepfine.inventorysystem.infrastructure.persistence.inventory;

import com.deepfine.inventorysystem.domain.inventory.InventoryRepository;
import com.deepfine.inventorysystem.domain.inventory.InventorySnapshot;
import com.deepfine.inventorysystem.infrastructure.persistence.inventory.InventoryJpaRepository.ChangedRow;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepository {

    private final InventoryJpaRepository inventoryJpaRepository;

    @Override
    public InventorySnapshot increase(Long productId, long quantity) {
        return toSnapshot(inventoryJpaRepository.upsertIncrease(productId, quantity));
    }

    /**
     * 엔티티로 읽은 {@code timestamptz}와 같게 UTC 오프셋으로 둔다.
     * 응답의 {@code +09:00} 표기는 Jackson({@code spring.jackson.time-zone})이 맞춘다.
     */
    private static InventorySnapshot toSnapshot(ChangedRow row) {
        return new InventorySnapshot(row.getQuantity(), row.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
}

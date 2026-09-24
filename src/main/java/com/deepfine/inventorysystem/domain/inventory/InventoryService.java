package com.deepfine.inventorysystem.domain.inventory;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.inventory.exception.InventoryException;
import com.deepfine.inventorysystem.support.properties.InventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final long MIN_QUANTITY = 1;

    private final InventoryRepository inventoryRepository;
    private final InventoryProperties inventoryProperties;

    public void validateInboundQuantity(long quantity) {
        if (quantity < MIN_QUANTITY || quantity > inventoryProperties.maxQuantity()) {
            throw new InventoryException(ErrorCode.INVALID_QUANTITY);
        }
    }

    public InventoryState increase(Long productId, long quantity) {
        return inventoryRepository.increase(productId, quantity);
    }

    public Inventory get(Long productId) {
        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException("상품은 있는데 재고가 없습니다."));
    }
}

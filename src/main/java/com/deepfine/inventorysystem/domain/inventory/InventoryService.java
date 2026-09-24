package com.deepfine.inventorysystem.domain.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * 재고 증가 <br>
     * - 재고 행이 없으면 입고 수량으로 만든다 <br>
     * - 이 요청이 반영된 직후의 수량과 변경 시각을 돌려준다 <br>
     */
    public InventoryState increase(Long productId, long quantity) {
        return inventoryRepository.increase(productId, quantity);
    }

    /**
     * 재고 차감 <br>
     * - 재고가 출고 수량 이상일 때만 빼고, 모자라면 재고를 그대로 두고 거부한다 <br>
     * - 이 요청이 반영된 직후의 수량과 변경 시각을 돌려준다 <br>
     */
    public InventoryState decrease(Long productId, long quantity) {
        return inventoryRepository.decrease(productId, quantity);
    }

    public Inventory get(Long productId) {
        return inventoryRepository
                .findByProductId(productId)
                .orElseThrow(() -> new IllegalStateException("상품은 있는데 재고가 없습니다."));
    }
}

package com.deepfine.inventorysystem.domain.inventory;

public interface InventoryRepository {

    /**
     * 재고를 원자적으로 더한다 (03 §10 UPSERT). 재고 행이 없으면 입고 수량으로 만든다.
     * 반영 직후의 수량과 변경 시각을 돌려준다.
     */
    InventoryState increase(Long productId, long quantity);
}

package com.deepfine.inventorysystem.domain.inventory;

public interface InventoryRepository {

    /**
     * 재고 증가 <br>
     * - 재고 행이 없으면 입고 수량으로 만든다 <br>
     * - 이 요청이 반영된 직후의 수량과 변경 시각을 돌려준다 <br>
     */
    InventoryState increase(Long productId, long quantity);
}

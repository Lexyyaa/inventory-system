package com.deepfine.inventorysystem.domain.inventory;

import com.deepfine.inventorysystem.domain.common.BaseTimeEntity;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.inventory.exception.InventoryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 <br>
 * - 생성과 수량 변경은 원자 SQL로만 해서 상태 변경 메서드를 두지 않는다 <br>
 */
@Entity
@Table(name = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseTimeEntity {

    private static final long MIN_QUANTITY = 1;

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;

    /**
     * 입고 수량 검사 <br>
     * - 상품을 만들기 전에 검사해서 재고 행이 없을 수 있어 정적 메서드로 둔다 <br>
     * - 상한은 설정값이라 호출하는 쪽이 넘긴다 <br>
     */
    public static void validateInboundQuantity(long quantity, long maxQuantity) {
        if (quantity < MIN_QUANTITY || quantity > maxQuantity) {
            throw new InventoryException(ErrorCode.INVALID_QUANTITY);
        }
    }
}

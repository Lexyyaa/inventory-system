package com.deepfine.inventorysystem.domain.inventory;

import com.deepfine.inventorysystem.domain.common.BaseTimeEntity;
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

    @Id
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private Long quantity;
}

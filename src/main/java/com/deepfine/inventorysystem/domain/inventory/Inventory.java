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
 * 상품 하나의 현재 재고. {@code product_id}가 PK라 상품 하나에 재고 행은 하나뿐이다.
 *
 * <p>재고 행의 생성과 수량 변경은 원자 SQL(03 §10 · §11)로만 한다.
 * 그래서 생성 팩토리와 상태 변경 메서드를 두지 않는다. {@code quantity >= 0}은 DB CHECK 제약이 최종 보장한다.
 * {@code updated_at}은 DB가 채운다.
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

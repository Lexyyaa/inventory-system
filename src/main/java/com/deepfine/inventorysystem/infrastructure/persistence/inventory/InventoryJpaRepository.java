package com.deepfine.inventorysystem.infrastructure.persistence.inventory;

import com.deepfine.inventorysystem.domain.inventory.Inventory;
import com.deepfine.inventorysystem.domain.inventory.InventoryState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryJpaRepository extends JpaRepository<Inventory, Long> {

    /**
     * 재고 증가 <br>
     * - 읽고 더해서 쓰지 않고 한 문장으로 더해 동시 입고의 증가분이 덮어써지지 않게 한다 <br>
     * - updated_at은 GREATEST로 과거로 돌아가지 않게 한다 <br>
     * - RETURNING 결과를 받아야 해서 @Modifying을 붙이지 않는다 <br>
     */
    @Query(value = """
            INSERT INTO inventory (product_id, quantity, updated_at)
            VALUES (:productId, :quantity, clock_timestamp())
            ON CONFLICT (product_id)
            DO UPDATE SET
                quantity = inventory.quantity + EXCLUDED.quantity,
                updated_at = GREATEST(inventory.updated_at, clock_timestamp())
            RETURNING quantity, updated_at AS updatedAt
            """, nativeQuery = true)
    InventoryState upsertIncrease(@Param("productId") Long productId, @Param("quantity") long quantity);
}

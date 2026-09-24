package com.deepfine.inventorysystem.infrastructure.persistence.inventory;

import com.deepfine.inventorysystem.domain.inventory.Inventory;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryJpaRepository extends JpaRepository<Inventory, Long> {

    /**
     * 03 §10의 재고 증가 UPSERT. 읽고 계산해 쓰지 않고 한 문장으로 더하므로 동시 입고의 증가분이 덮어써지지 않는다.
     * {@code updated_at}은 실제 실행 시각({@code clock_timestamp()})으로 두되 {@code GREATEST}로 뒤로 가지 않게 한다 (03 §7).
     * {@code RETURNING}이 필요해 {@code @Modifying}을 붙이지 않는다. 별칭은 {@link ChangedRow} 필드명과 맞춘다.
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
    ChangedRow upsertIncrease(@Param("productId") Long productId, @Param("quantity") long quantity);

    /**
     * 재고 변경 SQL의 {@code RETURNING} 한 행.
     * Hibernate는 native 쿼리 결과의 {@code timestamptz}를 {@link Instant}로 읽으므로
     * 도메인 record({@code OffsetDateTime})로 바로 받지 못하고 여기서 받아 RepositoryImpl이 바꾼다.
     */
    interface ChangedRow {
        Long getQuantity();

        Instant getUpdatedAt();
    }
}

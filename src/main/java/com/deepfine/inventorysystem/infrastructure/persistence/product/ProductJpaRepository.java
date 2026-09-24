package com.deepfine.inventorysystem.infrastructure.persistence.product;

import com.deepfine.inventorysystem.domain.product.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByTenantIdAndProductCode(Long tenantId, String productCode);

    /**
     * 03 §9의 상품 생성 SQL. 새로 만들었으면 id를, 이미 있어 아무것도 하지 않았으면 빈 값을 돌려준다.
     * 중복 예외를 내지 않으므로 트랜잭션이 오류 상태가 되지 않고 이어서 다시 조회할 수 있다.
     * {@code RETURNING}이 필요해 {@code @Modifying}을 붙이지 않는다.
     */
    @Query(value = """
            INSERT INTO product (tenant_id, product_code, name, created_at)
            VALUES (:tenantId, :productCode, :name, CURRENT_TIMESTAMP)
            ON CONFLICT (tenant_id, product_code) DO NOTHING
            RETURNING id
            """, nativeQuery = true)
    Optional<Long> insertIfAbsent(
            @Param("tenantId") Long tenantId, @Param("productCode") String productCode, @Param("name") String name);
}

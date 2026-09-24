package com.deepfine.inventorysystem.infrastructure.persistence.product;

import com.deepfine.inventorysystem.domain.product.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByTenantIdAndProductCode(Long tenantId, String productCode);

    /**
     * 상품 생성 시도 <br>
     * - 중복이면 예외 없이 넘어가서 같은 트랜잭션에서 이어서 조회할 수 있다 <br>
     * - RETURNING 결과를 받아야 해서 @Modifying을 붙이지 않는다 <br>
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

package com.deepfine.inventorysystem.domain.product;

import com.deepfine.inventorysystem.domain.common.BaseTimeEntity;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.product.exception.ProductException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 <br>
 * - 저장은 JPA save가 아니라 ON CONFLICT DO NOTHING 쿼리로 한다 <br>
 * - 상품명은 생성 뒤 바뀌지 않아 변경 메서드를 두지 않는다 <br>
 */
@Entity
@Table(
        name = "product",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_product_tenant_code",
                        columnNames = {"tenant_id", "product_code"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "product_code", nullable = false, length = 100)
    private String productCode;

    @Column(name = "name", nullable = false)
    private String name;

    private Product(Long tenantId, String productCode, String name) {
        this.tenantId = tenantId;
        this.productCode = productCode;
        this.name = name;
    }

    public static Product create(Long tenantId, String productCode, String name) {
        return new Product(tenantId, productCode, name);
    }

    public void validateName(String productName) {
        if (!name.equals(productName)) {
            throw new ProductException(ErrorCode.PRODUCT_NAME_MISMATCH);
        }
    }
}

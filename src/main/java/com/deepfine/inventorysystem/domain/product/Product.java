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
 * 업체 안에서 상품코드로 식별하는 상품. 재고 수량은 {@code Inventory}가 따로 갖는다.
 *
 * <p>DB 저장은 JPA {@code save}가 아니라 {@code INSERT ... ON CONFLICT DO NOTHING}(03 §9)으로 한다.
 * 상품명은 생성 뒤 바뀌지 않으므로 상태 변경 메서드를 두지 않는다. {@code created_at}은 DB가 채운다.
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

    /**
     * 요청 업체의 새 상품을 만든다. 형식(허용 문자 · 길이 · 공백) 검증은 요청 단계에서 끝났다고 본다.
     */
    public static Product create(Long tenantId, String productCode, String name) {
        return new Product(tenantId, productCode, name);
    }

    /**
     * 입고 요청의 상품명이 등록된 상품명과 같은지 확인한다. 대소문자와 공백까지 정확히 같아야 같은 상품명이다.
     * 다르면 PRODUCT_NAME_MISMATCH이고, 등록된 상품명은 바꾸지 않는다.
     */
    public void validateName(String productName) {
        if (!name.equals(productName)) {
            throw new ProductException(ErrorCode.PRODUCT_NAME_MISMATCH);
        }
    }
}

package com.deepfine.inventorysystem.infrastructure.persistence.product;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import com.deepfine.inventorysystem.domain.product.Product;
import com.deepfine.inventorysystem.domain.product.ProductRepository;
import com.deepfine.inventorysystem.domain.product.exception.ProductException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    /**
     * 새로 만들었는지(RETURNING id가 있는지)와 상관없이 호출하는 쪽이 같은 트랜잭션에서 다시 조회한다.
     * 신규 · 기존 상품을 한 경로로 읽어 상품명을 검증하기 위해서다.
     */
    @Override
    public void createIfAbsent(Product product) {
        productJpaRepository.insertIfAbsent(product.getTenantId(), product.getProductCode(), product.getName());
    }

    @Override
    public Product getByTenantIdAndProductCode(Long tenantId, String productCode) {
        return productJpaRepository
                .findByTenantIdAndProductCode(tenantId, productCode)
                .orElseThrow(() -> new ProductException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    @Override
    public Optional<Product> findByTenantIdAndProductCode(Long tenantId, String productCode) {
        return productJpaRepository.findByTenantIdAndProductCode(tenantId, productCode);
    }
}

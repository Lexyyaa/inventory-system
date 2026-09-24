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

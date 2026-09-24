package com.deepfine.inventorysystem.domain.product;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    /**
     * 입고할 상품 확보 <br>
     * - 없으면 만들고, 있으면 기존 상품을 쓴다. 동시에 만들어도 하나만 남는다 <br>
     * - 기존 상품이면 상품명이 같아야 한다 <br>
     */
    public Product getOrCreate(Long tenantId, String productCode, String productName) {
        productRepository.createIfAbsent(Product.create(tenantId, productCode, productName));
        Product product = productRepository
                .findByTenantIdAndProductCode(tenantId, productCode)
                .orElseThrow(() -> new IllegalStateException("생성을 시도한 상품을 다시 읽지 못했습니다."));
        product.validateName(productName);
        return product;
    }

    public Product get(Long tenantId, String productCode) {
        return productRepository.getByTenantIdAndProductCode(tenantId, productCode);
    }
}

package com.deepfine.inventorysystem.domain.product;

import java.util.Optional;

public interface ProductRepository {

    /**
     * 상품 생성 시도 <br>
     * - 같은 업체에 같은 상품코드가 있으면 아무것도 하지 않는다 <br>
     * - 동시에 만든 요청이 있으면 그 요청이 끝날 때까지 기다린다 <br>
     */
    void createIfAbsent(Product product);

    Product getByTenantIdAndProductCode(Long tenantId, String productCode);

    Optional<Product> findByTenantIdAndProductCode(Long tenantId, String productCode);
}

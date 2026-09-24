package com.deepfine.inventorysystem.domain.product;

import java.util.Optional;

public interface ProductRepository {

    void createIfAbsent(Product product);

    Product getByTenantIdAndProductCode(Long tenantId, String productCode);

    Optional<Product> findByTenantIdAndProductCode(Long tenantId, String productCode);
}

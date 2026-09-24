package com.deepfine.inventorysystem.domain.product;

public interface ProductRepository {

    /**
     * 같은 업체에 같은 상품코드가 없을 때만 상품을 저장한다 (03 §9 {@code INSERT ... ON CONFLICT DO NOTHING}).
     * 이미 있으면 아무것도 하지 않는다. 동시에 만든 다른 요청이 있으면 그 요청이 끝날 때까지 기다린다.
     * 저장된 상품은 같은 트랜잭션에서 {@link #getByTenantIdAndProductCode}로 다시 읽는다.
     */
    void createIfAbsent(Product product);

    /** 업체와 상품코드로 상품을 찾는다. 없으면 PRODUCT_NOT_FOUND. */
    Product getByTenantIdAndProductCode(Long tenantId, String productCode);
}

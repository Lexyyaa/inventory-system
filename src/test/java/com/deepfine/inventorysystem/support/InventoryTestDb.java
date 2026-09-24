package com.deepfine.inventorysystem.support;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 상품 · 재고 테스트 데이터를 API 없이 DB에 직접 넣고 읽는다.
 * 조회는 모두 업체 코드 + 상품코드로 걸러 seed나 다른 테스트 데이터와 섞이지 않게 한다.
 * seed 업체(tenant)는 건드리지 않는다.
 */
@RequiredArgsConstructor
public class InventoryTestDb {

    private final JdbcTemplate jdbcTemplate;

    /** 테스트마다 product · inventory만 비운다. */
    public void clear() {
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM product");
    }

    public Long tenantId(String tenantCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM tenant WHERE code = ?", Long.class, tenantCode);
    }

    /** 상품과 재고 행을 함께 넣고 상품 id를 돌려준다. */
    public Long insertProduct(String tenantCode, String productCode, String name, long quantity) {
        Long productId = jdbcTemplate.queryForObject(
                "INSERT INTO product (tenant_id, product_code, name) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                tenantId(tenantCode),
                productCode,
                name);
        jdbcTemplate.update("INSERT INTO inventory (product_id, quantity) VALUES (?, ?)", productId, quantity);
        return productId;
    }

    /** 업체 · 상품코드의 상품명 목록. 행 수가 곧 상품 건수다. */
    public List<String> productNames(String tenantCode, String productCode) {
        return jdbcTemplate.queryForList("""
                SELECT p.name
                FROM product p
                JOIN tenant t ON t.id = p.tenant_id
                WHERE t.code = ? AND p.product_code = ?
                """, String.class, tenantCode, productCode);
    }

    public long productCount(String tenantCode) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM product p
                JOIN tenant t ON t.id = p.tenant_id
                WHERE t.code = ?
                """, Long.class, tenantCode);
    }

    /** 업체 · 상품코드의 재고 수량 목록. 행 수가 곧 재고 행 건수다. */
    public List<Long> inventoryQuantities(String tenantCode, String productCode) {
        return jdbcTemplate.queryForList("""
                SELECT i.quantity
                FROM inventory i
                JOIN product p ON p.id = i.product_id
                JOIN tenant t ON t.id = p.tenant_id
                WHERE t.code = ? AND p.product_code = ?
                """, Long.class, tenantCode, productCode);
    }

    public OffsetDateTime inventoryUpdatedAt(String tenantCode, String productCode) {
        return jdbcTemplate.queryForObject("""
                SELECT i.updated_at
                FROM inventory i
                JOIN product p ON p.id = i.product_id
                JOIN tenant t ON t.id = p.tenant_id
                WHERE t.code = ? AND p.product_code = ?
                """, OffsetDateTime.class, tenantCode, productCode);
    }
}

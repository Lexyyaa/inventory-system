package com.deepfine.inventorysystem.support;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;

@RequiredArgsConstructor
public class InventoryTestDb {

    private final JdbcTemplate jdbcTemplate;

    public void clear() {
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM product");
    }

    public Long tenantId(String tenantCode) {
        return jdbcTemplate.queryForObject("SELECT id FROM tenant WHERE code = ?", Long.class, tenantCode);
    }

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

package com.deepfine.inventorysystem.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.deepfine.inventorysystem.support.IntegrationTest;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * schema.sql의 UNIQUE · CHECK 제약을 API 없이 DB에 직접 저장해 확인한다.
 * 테스트 메서드에 트랜잭션을 걸지 않는다. JdbcTemplate이 문장마다 커밋하므로 제약 위반 뒤에도 다음 문장과 확인 조회가 실행된다.
 */
@IntegrationTest
class SchemaConstraintTest {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String CHECK_VIOLATION = "23514";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM inventory");
        jdbcTemplate.update("DELETE FROM product");
    }

    @Test
    @DisplayName("[TC-1-03] 같은 업체에 같은 상품코드의 상품을 두 번 저장하면 유일 제약 위반으로 두 번째 저장이 실패한다")
    void rejectsDuplicateProductCodeInSameTenant() {
        // given
        Long tenantId = tenantId("tenant-001");
        insertProduct(tenantId, "A001", "Apple");

        // when
        Throwable thrown = catchThrowable(() -> insertProduct(tenantId, "A001", "Samsung"));

        // then
        assertThat(thrown).isInstanceOf(DuplicateKeyException.class);
        assertThat(sqlState(thrown)).isEqualTo(UNIQUE_VIOLATION);
        assertThat(productNames("tenant-001", "A001")).containsExactly("Apple");
    }

    @Test
    @DisplayName("[TC-1-04] 재고를 음수로 저장하면 CHECK 제약 위반으로 저장이 실패하고 기존 재고를 유지한다")
    void rejectsNegativeInventoryQuantity() {
        // given
        Long tenantId = tenantId("tenant-001");
        Long appleId = insertProduct(tenantId, "A001", "Apple");
        Long bananaId = insertProduct(tenantId, "B001", "Banana");
        jdbcTemplate.update("INSERT INTO inventory (product_id, quantity) VALUES (?, ?)", bananaId, 10L);

        // when
        Throwable negativeInsert = catchThrowable(
                () -> jdbcTemplate.update("INSERT INTO inventory (product_id, quantity) VALUES (?, ?)", appleId, -1L));
        Throwable belowZeroUpdate = catchThrowable(() ->
                jdbcTemplate.update("UPDATE inventory SET quantity = quantity - 11 WHERE product_id = ?", bananaId));

        // then
        assertThat(negativeInsert).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sqlState(negativeInsert)).isEqualTo(CHECK_VIOLATION);
        assertThat(belowZeroUpdate).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sqlState(belowZeroUpdate)).isEqualTo(CHECK_VIOLATION);
        assertThat(inventoryQuantities("tenant-001", "A001")).isEmpty();
        assertThat(inventoryQuantities("tenant-001", "B001")).containsExactly(10L);
    }

    private Long tenantId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM tenant WHERE code = ?", Long.class, code);
    }

    private Long insertProduct(Long tenantId, String productCode, String name) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO product (tenant_id, product_code, name) VALUES (?, ?, ?) RETURNING id",
                Long.class,
                tenantId,
                productCode,
                name);
    }

    private List<String> productNames(String tenantCode, String productCode) {
        return jdbcTemplate.queryForList("""
                SELECT p.name
                FROM product p
                JOIN tenant t ON t.id = p.tenant_id
                WHERE t.code = ? AND p.product_code = ?
                """, String.class, tenantCode, productCode);
    }

    private List<Long> inventoryQuantities(String tenantCode, String productCode) {
        return jdbcTemplate.queryForList("""
                SELECT i.quantity
                FROM inventory i
                JOIN product p ON p.id = i.product_id
                JOIN tenant t ON t.id = p.tenant_id
                WHERE t.code = ? AND p.product_code = ?
                """, Long.class, tenantCode, productCode);
    }

    private static String sqlState(Throwable thrown) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(thrown);
        return cause instanceof SQLException sqlException ? sqlException.getSQLState() : null;
    }
}

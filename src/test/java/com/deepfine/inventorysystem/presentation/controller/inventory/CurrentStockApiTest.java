package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deepfine.inventorysystem.support.IntegrationTest;
import com.deepfine.inventorysystem.support.InventoryTestDb;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class CurrentStockApiTest {

    private static final String CURRENT_STOCK_URL = "/api/v1/inventory/{productCode}";

    private static final String PRODUCT_NOT_FOUND_MESSAGE = "상품을 찾을 수 없습니다.";
    private static final ZoneOffset SEOUL_OFFSET = ZoneOffset.ofHours(9);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    InventoryTestDb db;

    @BeforeEach
    void setUp() {
        db = new InventoryTestDb(jdbcTemplate);
        db.clear();
    }

    @Test
    @DisplayName("[TC-3-01] 등록된 상품을 조회하면 200으로 상품코드, 상품명, 재고 수량, 마지막 변경 시각을 반환하고 내부 식별자는 반환하지 않는다")
    void returnsCurrentStockWithoutInternalIds() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        OffsetDateTime updatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");

        // when
        ResultActions result = currentStock("tenant-001", "A001");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.productId").doesNotExist())
                .andExpect(jsonPath("$.tenantId").doesNotExist());
        assertThat(bodyOf(result)).containsOnlyKeys("productCode", "productName", "quantity", "updatedAt");
        OffsetDateTime updatedAt = updatedAtOf(result);
        assertThat(updatedAt.getOffset()).isEqualTo(SEOUL_OFFSET);
        assertThat(updatedAt).isAtSameInstantAs(updatedAtBefore);
        assertThat(db.productCount("tenant-001")).isEqualTo(1);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.inventoryUpdatedAt("tenant-001", "A001")).isAtSameInstantAs(updatedAtBefore);
    }

    @Test
    @DisplayName("[TC-3-02] tenant-001과 tenant-002가 각자의 A001을 조회하면 각각 200과 자기 업체의 재고 10과 20을 반환한다")
    void returnsEachTenantsOwnStock() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        db.insertProduct("tenant-002", "A001", "Samsung", 20);
        OffsetDateTime tenant001UpdatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");
        OffsetDateTime tenant002UpdatedAtBefore = db.inventoryUpdatedAt("tenant-002", "A001");

        // when
        ResultActions tenant001 = currentStock("tenant-001", "A001");
        ResultActions tenant002 = currentStock("tenant-002", "A001");

        // then
        tenant001
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(10));
        assertThat(updatedAtOf(tenant001).getOffset()).isEqualTo(SEOUL_OFFSET);
        tenant002
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Samsung"))
                .andExpect(jsonPath("$.quantity").value(20));
        assertThat(updatedAtOf(tenant002).getOffset()).isEqualTo(SEOUL_OFFSET);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.inventoryUpdatedAt("tenant-001", "A001")).isAtSameInstantAs(tenant001UpdatedAtBefore);
        assertThat(db.productNames("tenant-002", "A001")).containsExactly("Samsung");
        assertThat(db.inventoryQuantities("tenant-002", "A001")).containsExactly(20L);
        assertThat(db.inventoryUpdatedAt("tenant-002", "A001")).isAtSameInstantAs(tenant002UpdatedAtBefore);
    }

    @Test
    @DisplayName("[TC-3-03] 요청 업체에 등록되지 않은 상품코드를 조회하면 404 PRODUCT_NOT_FOUND를 반환하고 재고 정보를 반환하지 않는다")
    void rejectsUnregisteredProductCode() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        assertThat(db.productNames("tenant-001", "B001")).isEmpty();

        // when
        ResultActions result = currentStock("tenant-001", "B001");

        // then
        expectProductNotFound(result);
        assertThat(db.productCount("tenant-001")).isEqualTo(1);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.productNames("tenant-001", "B001")).isEmpty();
        assertThat(db.productNames("tenant-002", "B001")).isEmpty();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    @Test
    @DisplayName(
            "[TC-3-04] tenant-002가 tenant-001에만 있는 A001을 조회하면 404 PRODUCT_NOT_FOUND를 반환하고 tenant-001의 재고는 노출하지 않는다")
    void rejectsProductOnlyInOtherTenant() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        OffsetDateTime updatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");
        assertThat(db.productCount("tenant-002")).isZero();

        // when
        ResultActions result = currentStock("tenant-002", "A001");

        // then
        expectProductNotFound(result);
        assertThat(db.productCount("tenant-002")).isZero();
        assertThat(db.productCount("tenant-001")).isEqualTo(1);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.inventoryUpdatedAt("tenant-001", "A001")).isAtSameInstantAs(updatedAtBefore);
    }

    @Test
    @DisplayName("[TC-3-05] 허용 문자나 길이를 벗어난 상품코드로 조회하면 형식 오류가 아니라 404 PRODUCT_NOT_FOUND를 반환한다")
    void returnsNotFoundForOutOfFormatProductCode() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);

        // when
        ResultActions overLength = currentStock("tenant-001", "A".repeat(101));
        ResultActions withSpace = currentStock("tenant-001", "A 001");

        // then
        expectProductNotFound(overLength);
        expectProductNotFound(withSpace);
        assertThat(db.productCount("tenant-001")).isEqualTo(1);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    @Test
    @DisplayName("[TC-3-06] 상품코드는 대소문자를 구분해 A001만 있을 때 a001로 조회하면 404 PRODUCT_NOT_FOUND를 반환한다")
    void distinguishesProductCodeCase() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);

        // when
        ResultActions result = currentStock("tenant-001", "a001");

        // then
        expectProductNotFound(result);
        assertThat(db.productCount("tenant-001")).isEqualTo(1);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    private ResultActions currentStock(String tenantCode, String productCode) throws Exception {
        return mockMvc.perform(get(CURRENT_STOCK_URL, productCode).header(TENANT_HEADER, tenantCode));
    }

    private static void expectProductNotFound(ResultActions result) throws Exception {
        result.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value(PRODUCT_NOT_FOUND_MESSAGE))
                .andExpect(jsonPath("$.productCode").doesNotExist())
                .andExpect(jsonPath("$.productName").doesNotExist())
                .andExpect(jsonPath("$.quantity").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());
        assertThat(bodyOf(result)).containsOnlyKeys("code", "message");
    }

    private static OffsetDateTime updatedAtOf(ResultActions result) throws Exception {
        String updatedAt = (String) bodyOf(result).get("updatedAt");
        return OffsetDateTime.parse(updatedAt);
    }

    private static Map<String, Object> bodyOf(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$");
    }
}

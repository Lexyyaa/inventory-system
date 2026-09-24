package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class OutboundApiTest {

    private static final String OUTBOUND_URL = "/api/v1/inventory/outbound";
    private static final String CURRENT_STOCK_URL = "/api/v1/inventory/{productCode}";

    private static final String INVALID_QUANTITY_MESSAGE = "출고 수량이 허용 범위를 벗어났습니다.";
    private static final String PRODUCT_NOT_FOUND_MESSAGE = "상품을 찾을 수 없습니다.";
    private static final String INSUFFICIENT_STOCK_MESSAGE = "출고 수량이 현재 재고보다 많습니다.";
    private static final String MISSING_MESSAGE = "필수 요청 정보가 누락되었습니다.";
    private static final String MALFORMED_MESSAGE = "요청 형식이 올바르지 않습니다.";
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
    @DisplayName("[TC-4-01] 재고 100에서 30을 출고하면 200과 출고 직후 재고 70을 반환한다")
    void decreasesStockAndReturnsQuantityAfterOutbound() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 100);

        // when
        ResultActions result = outbound("tenant-001", """
                {"productCode":"A001","quantity":30}""");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(70))
                .andExpect(jsonPath("$.id").doesNotExist());
        assertThat(bodyOf(result)).containsOnlyKeys("productCode", "productName", "quantity", "updatedAt");
        OffsetDateTime updatedAt = updatedAtOf(result);
        assertThat(updatedAt.getOffset()).isEqualTo(SEOUL_OFFSET);
        ResultActions stock = currentStock("tenant-001", "A001");
        stock.andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(70));
        assertThat(updatedAtOf(stock)).isAtSameInstantAs(updatedAt);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(70L);
        assertThat(db.inventoryUpdatedAt("tenant-001", "A001")).isAtSameInstantAs(updatedAt);
    }

    @Test
    @DisplayName("[TC-4-02] 등록되지 않은 상품을 출고하면 PRODUCT_NOT_FOUND로 거부하고 상품을 만들지 않는다")
    void rejectsUnregisteredProduct() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();

        // when
        ResultActions result = outbound("tenant-001", """
                {"productCode":"A001","quantity":10}""");

        // then
        expectError(result, HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", PRODUCT_NOT_FOUND_MESSAGE);
        expectError(
                currentStock("tenant-001", "A001"),
                HttpStatus.NOT_FOUND,
                "PRODUCT_NOT_FOUND",
                PRODUCT_NOT_FOUND_MESSAGE);
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.productCount("tenant-001")).isZero();
    }

    @Test
    @DisplayName("[TC-4-03] 재고 10에서 11을 출고하면 INSUFFICIENT_STOCK으로 거부하고 재고를 그대로 둔다")
    void rejectsOutboundOverStock() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        OffsetDateTime updatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");

        // when
        ResultActions result = outbound("tenant-001", """
                {"productCode":"A001","quantity":11}""");

        // then
        expectError(result, HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", INSUFFICIENT_STOCK_MESSAGE);
        ResultActions stock = currentStock("tenant-001", "A001");
        stock.andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(10));
        assertThat(updatedAtOf(stock)).isAtSameInstantAs(updatedAtBefore);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.inventoryUpdatedAt("tenant-001", "A001")).isAtSameInstantAs(updatedAtBefore);
    }

    @Test
    @DisplayName("[TC-4-04] 출고 수량이 0 또는 -1이면 INVALID_QUANTITY로 거부하고 재고를 유지한다")
    void rejectsZeroAndNegativeQuantity() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        OffsetDateTime updatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");

        // when
        ResultActions zero = outbound("tenant-001", """
                {"productCode":"A001","quantity":0}""");
        ResultActions negative = outbound("tenant-001", """
                {"productCode":"A001","quantity":-1}""");

        // then
        expectError(zero, HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        expectError(negative, HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        ResultActions stock = currentStock("tenant-001", "A001");
        stock.andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(10));
        assertThat(updatedAtOf(stock)).isAtSameInstantAs(updatedAtBefore);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    @Test
    @DisplayName("[TC-4-05] 재고 10에서 10을 출고하면 200과 재고 0을 반환하고 상품은 계속 조회된다")
    void allowsOutboundOfEntireStock() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);

        // when
        ResultActions result = outbound("tenant-001", """
                {"productCode":"A001","quantity":10}""");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(0));
        assertThat(updatedAtOf(result).getOffset()).isEqualTo(SEOUL_OFFSET);
        currentStock("tenant-001", "A001")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(0));
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(0L);
    }

    @Test
    @DisplayName("[TC-4-06] tenant-002가 tenant-001에만 있는 A001을 출고하면 PRODUCT_NOT_FOUND로 거부하고 tenant-001의 재고를 유지한다")
    void rejectsProductOnlyInOtherTenant() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        OffsetDateTime updatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");
        assertThat(db.productNames("tenant-002", "A001")).isEmpty();

        // when
        ResultActions result = outbound("tenant-002", """
                {"productCode":"A001","quantity":5}""");

        // then
        expectError(result, HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", PRODUCT_NOT_FOUND_MESSAGE);
        ResultActions tenant001 = currentStock("tenant-001", "A001");
        tenant001
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.productName").value("Apple"));
        assertThat(updatedAtOf(tenant001)).isAtSameInstantAs(updatedAtBefore);
        ResultActions tenant002 = currentStock("tenant-002", "A001");
        expectError(tenant002, HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", PRODUCT_NOT_FOUND_MESSAGE);
        assertThat(db.productNames("tenant-002", "A001")).isEmpty();
        assertThat(db.productCount("tenant-002")).isZero();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    @Test
    @DisplayName("[TC-4-11] 등록되지 않은 상품에 수량 0으로 출고하면 상품 없음보다 수량 오류가 먼저라 400 INVALID_QUANTITY로 거부한다")
    void checksQuantityBeforeProductExistence() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();

        // when
        ResultActions result = outbound("tenant-001", """
                {"productCode":"A001","quantity":0}""");

        // then
        expectError(result, HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        assertThat(db.productCount("tenant-001")).isZero();
    }

    @Test
    @DisplayName("[TC-4-12] 출고 요청의 필수값 누락과 정수가 아닌 수량은 INVALID_REQUEST, 상한 초과는 INVALID_QUANTITY로 거부하고 재고를 유지한다")
    void rejectsInvalidOutboundRequest() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);

        // when
        ResultActions missingCode = outbound("tenant-001", """
                {"quantity":5}""");
        ResultActions fractional = outbound("tenant-001", """
                {"productCode":"A001","quantity":1.5}""");
        ResultActions overMax = outbound("tenant-001", """
                {"productCode":"A001","quantity":1000000001}""");

        // then
        expectError(missingCode, HttpStatus.BAD_REQUEST, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(fractional, HttpStatus.BAD_REQUEST, "INVALID_REQUEST", MALFORMED_MESSAGE);
        expectError(overMax, HttpStatus.BAD_REQUEST, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    private ResultActions outbound(String tenantCode, String body) throws Exception {
        return mockMvc.perform(post(OUTBOUND_URL)
                .header(TENANT_HEADER, tenantCode)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions currentStock(String tenantCode, String productCode) throws Exception {
        return mockMvc.perform(get(CURRENT_STOCK_URL, productCode).header(TENANT_HEADER, tenantCode));
    }

    private static void expectError(ResultActions result, HttpStatus status, String code, String message)
            throws Exception {
        result.andExpect(status().is(status.value()))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message));
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

package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deepfine.inventorysystem.support.IntegrationTest;
import com.deepfine.inventorysystem.support.InventoryTestDb;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@IntegrationTest
class InboundApiTest {

    private static final String INBOUND_URL = "/api/v1/inventory/inbound";

    // 04 §3 "오류 응답" 문구
    private static final String INVALID_QUANTITY_MESSAGE = "입고 수량이 허용 범위를 벗어났습니다.";
    private static final String MISSING_MESSAGE = "필수 요청 정보가 누락되었습니다.";
    private static final String MALFORMED_MESSAGE = "요청 형식이 올바르지 않습니다.";
    private static final String INVALID_TENANT_MESSAGE = "Tenant 정보가 없거나 등록되지 않았습니다.";
    private static final String NAME_MISMATCH_MESSAGE = "동일한 상품코드에 등록된 상품명과 일치하지 않습니다.";

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
    @DisplayName("[TC-2-01] 등록되지 않은 상품코드로 입고하면 요청 업체의 상품을 만들고 입고 수량을 재고로 반영한다")
    void createsProductForNewProductCode() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.productNames("tenant-002", "A001")).isEmpty();

        // when
        ResultActions result = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":10}""");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.id").doesNotExist());
        expectSeoulOffset(result);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.productNames("tenant-002", "A001")).isEmpty();
    }

    @Test
    @DisplayName("[TC-2-02] 재고 100인 기존 상품에 30을 입고하면 재고가 130이 된다")
    void addsQuantityToExistingProduct() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 100);

        // when
        ResultActions result = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":30}""");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Apple"))
                .andExpect(jsonPath("$.quantity").value(130));
        expectSeoulOffset(result);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(130L);
    }

    @Test
    @DisplayName("[TC-2-03] 다른 업체에만 있는 상품코드로 입고하면 요청 업체의 신규 상품을 만들고 다른 업체의 상품과 재고는 바꾸지 않는다")
    void createsProductForTenantWhenOnlyOtherTenantHasCode() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        assertThat(db.productNames("tenant-002", "A001")).isEmpty();

        // when
        ResultActions result = inbound("tenant-002", """
                {"productCode":"A001","productName":"Samsung","quantity":5}""");

        // then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value("Samsung"))
                .andExpect(jsonPath("$.quantity").value(5));
        expectSeoulOffset(result);
        assertThat(db.productNames("tenant-002", "A001")).containsExactly("Samsung");
        assertThat(db.inventoryQuantities("tenant-002", "A001")).containsExactly(5L);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
    }

    @Test
    @DisplayName("[TC-2-04] 기존 상품과 다른 상품명으로 입고하면 PRODUCT_NAME_MISMATCH로 거부하고 재고와 상품명을 유지한다")
    void rejectsDifferentProductName() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        OffsetDateTime updatedAtBefore = db.inventoryUpdatedAt("tenant-001", "A001");

        // when
        ResultActions result = inbound("tenant-001", """
                {"productCode":"A001","productName":"Samsung","quantity":5}""");

        // then
        result.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_NAME_MISMATCH"))
                .andExpect(jsonPath("$.message").value(NAME_MISMATCH_MESSAGE));
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.inventoryUpdatedAt("tenant-001", "A001")).isEqualTo(updatedAtBefore);
    }

    @Test
    @DisplayName("[TC-2-06] 입고 수량이 0이나 -1이거나 상한 1,000,000,000을 넘으면 INVALID_QUANTITY로 거부하고 상품을 만들지 않는다")
    void rejectsQuantityOutOfRange() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();

        // when
        ResultActions zero = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":0}""");
        ResultActions negative = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":-1}""");
        ResultActions overLimit = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":1000000001}""");

        // then
        expectError(zero, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        expectError(negative, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        expectError(overLimit, "INVALID_QUANTITY", INVALID_QUANTITY_MESSAGE);
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).isEmpty();
    }

    @Test
    @DisplayName(
            "[TC-2-07] productCode·productName·quantity가 없거나 productCode·productName이 빈 문자열이거나 productName이 공백뿐이면 INVALID_REQUEST로 거부한다")
    void rejectsMissingOrBlankFields() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        long productCountBefore = db.productCount("tenant-001");

        // when
        ResultActions missingCode = inbound("tenant-001", """
                {"productName":"Apple","quantity":10}""");
        ResultActions missingName = inbound("tenant-001", """
                {"productCode":"A001","quantity":10}""");
        ResultActions missingQuantity = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple"}""");
        ResultActions emptyCode = inbound("tenant-001", """
                {"productCode":"","productName":"Apple","quantity":10}""");
        ResultActions emptyName = inbound("tenant-001", """
                {"productCode":"A001","productName":"","quantity":10}""");
        ResultActions blankName = inbound("tenant-001", """
                {"productCode":"A001","productName":"   ","quantity":10}""");

        // then
        expectError(missingCode, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(missingName, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(missingQuantity, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(emptyCode, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(emptyName, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(blankName, "INVALID_REQUEST", MISSING_MESSAGE);
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.productCount("tenant-001")).isEqualTo(productCountBefore);
    }

    @Test
    @DisplayName("[TC-2-08] 수량이 1.5나 문자열 \"10\"이거나 JSON이 깨졌으면 INVALID_REQUEST로 거부하고, 헤더까지 없으면 INVALID_TENANT가 먼저다")
    void rejectsMalformedBodyAndChecksTenantFirst() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        String brokenJson = """
                {"productCode":"A001","productName":"Apple","quantity":10""";

        // when
        ResultActions decimal = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":1.5}""");
        ResultActions string = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":"10"}""");
        ResultActions broken = inbound("tenant-001", brokenJson);
        ResultActions brokenWithoutTenant = inbound(null, brokenJson);

        // then
        expectError(decimal, "INVALID_REQUEST", MALFORMED_MESSAGE);
        expectError(string, "INVALID_REQUEST", MALFORMED_MESSAGE);
        expectError(broken, "INVALID_REQUEST", MALFORMED_MESSAGE);
        expectError(brokenWithoutTenant, "INVALID_TENANT", INVALID_TENANT_MESSAGE);
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).isEmpty();
    }

    @Test
    @DisplayName("[TC-2-09] 상품코드 100자와 상품명 255자는 받고, 101자 상품코드나 256자 상품명은 INVALID_REQUEST로 거부한다")
    void acceptsMaxLengthAndRejectsOverLength() throws Exception {
        // given
        String code100 = "C".repeat(100);
        String name255 = "N".repeat(255);
        String code101 = "D".repeat(101);
        String name256 = "N".repeat(256);
        assertThat(db.productNames("tenant-001", code100)).isEmpty();
        assertThat(db.productNames("tenant-001", code101)).isEmpty();
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();

        // when
        ResultActions maxLength = inbound("tenant-001", body(code100, name255, 10));
        ResultActions codeOverLength = inbound("tenant-001", body(code101, "Apple", 10));
        ResultActions nameOverLength = inbound("tenant-001", body("A001", name256, 10));

        // then
        maxLength
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value(code100))
                .andExpect(jsonPath("$.productName").value(name255))
                .andExpect(jsonPath("$.quantity").value(10));
        expectSeoulOffset(maxLength);
        expectError(codeOverLength, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(nameOverLength, "INVALID_REQUEST", MISSING_MESSAGE);
        assertThat(db.productNames("tenant-001", code100)).containsExactly(name255);
        assertThat(db.inventoryQuantities("tenant-001", code100)).containsExactly(10L);
        assertThat(db.productNames("tenant-001", code101)).isEmpty();
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
    }

    // 05에 없는 케이스 (TC 추가 제안): 수량 범위의 양 끝 값은 받는다
    @Test
    @DisplayName("입고 수량이 하한 1이나 상한 1,000,000,000과 같으면 받는다")
    void acceptsQuantityAtBothBounds() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.productNames("tenant-001", "B001")).isEmpty();

        // when
        ResultActions min = inbound("tenant-001", """
                {"productCode":"A001","productName":"Apple","quantity":1}""");
        ResultActions max = inbound("tenant-001", """
                {"productCode":"B001","productName":"Banana","quantity":1000000000}""");

        // then
        min.andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(1));
        max.andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(1_000_000_000L));
        expectSeoulOffset(min);
        expectSeoulOffset(max);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(1L);
        assertThat(db.inventoryQuantities("tenant-001", "B001")).containsExactly(1_000_000_000L);
    }

    // 05에 없는 케이스 (TC 추가 제안): 상품명 길이는 DB VARCHAR(255)와 같게 문자(코드포인트) 단위로 센다
    @Test
    @DisplayName("이모지처럼 UTF-16 두 단위인 문자 255자 상품명은 받고, 256자는 INVALID_REQUEST로 거부한다")
    void countsProductNameLengthByCharacter() throws Exception {
        // given
        String emojiName255 = "😀".repeat(255);
        String emojiName256 = "😀".repeat(256);
        assertThat(emojiName255.length()).isEqualTo(510);
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.productNames("tenant-001", "B001")).isEmpty();

        // when
        ResultActions maxLength = inbound("tenant-001", body("A001", emojiName255, 10));
        ResultActions overLength = inbound("tenant-001", body("B001", emojiName256, 10));

        // then
        maxLength
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("A001"))
                .andExpect(jsonPath("$.productName").value(emojiName255))
                .andExpect(jsonPath("$.quantity").value(10));
        expectSeoulOffset(maxLength);
        expectError(overLength, "INVALID_REQUEST", MISSING_MESSAGE);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly(emojiName255);
        String storedName = db.productNames("tenant-001", "A001").getFirst();
        assertThat(storedName.codePointCount(0, storedName.length())).isEqualTo(255);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(10L);
        assertThat(db.productNames("tenant-001", "B001")).isEmpty();
    }

    // 05에 없는 케이스 (TC 추가 제안): PostgreSQL 문자열에 저장할 수 없는 상품명은 DB까지 가지 않고 400이다
    @Test
    @DisplayName("상품명에 NUL 문자나 짝 없는 서로게이트가 있으면 500이 아니라 INVALID_REQUEST로 거부하고 상품을 만들지 않는다")
    void rejectsProductNameNotStorableInDatabase() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();

        // when
        ResultActions nul = inbound("tenant-001", """
                {"productCode":"A001","productName":"App\\u0000le","quantity":10}""");
        ResultActions loneSurrogate = inbound("tenant-001", """
                {"productCode":"A001","productName":"App\\uD800le","quantity":10}""");

        // then
        expectError(nul, "INVALID_REQUEST", MISSING_MESSAGE);
        expectError(loneSurrogate, "INVALID_REQUEST", MISSING_MESSAGE);
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
    }

    /** tenantCode가 null이면 X-Tenant-Id 헤더를 넣지 않는다. */
    private ResultActions inbound(String tenantCode, String body) throws Exception {
        MockHttpServletRequestBuilder request =
                post(INBOUND_URL).contentType(MediaType.APPLICATION_JSON).content(body);
        if (tenantCode != null) {
            request.header(TENANT_HEADER, tenantCode);
        }
        return mockMvc.perform(request);
    }

    private static String body(String productCode, String productName, long quantity) {
        return """
                {"productCode":"%s","productName":"%s","quantity":%d}""".formatted(productCode, productName, quantity);
    }

    private static void expectError(ResultActions result, String code, String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message));
    }

    /** 성공 응답의 updatedAt은 +09:00 오프셋이 붙은 ISO-8601이다 (05 머리말). */
    private static void expectSeoulOffset(ResultActions result) throws Exception {
        String body = result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String updatedAt = JsonPath.read(body, "$.updatedAt");
        assertThat(OffsetDateTime.parse(updatedAt).getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }
}

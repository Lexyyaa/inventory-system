package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.deepfine.inventorysystem.support.ConcurrencyRunner;
import com.deepfine.inventorysystem.support.IntegrationTest;
import com.deepfine.inventorysystem.support.InventoryTestDb;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@IntegrationTest
class InboundConcurrencyTest {

    private static final String INBOUND_URL = "/api/v1/inventory/inbound";

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

    @AfterEach
    void tearDown() {
        db.clear();
    }

    @Test
    @DisplayName("[TC-2-11] 재고 100인 상품에 10, 20, 30을 동시에 입고하면 모두 성공하고 재고가 160이 된다")
    void concurrentInboundToExistingProduct() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 100);
        List<String> bodies = List.of("""
                {"productCode":"A001","productName":"Apple","quantity":10}""", """
                {"productCode":"A001","productName":"Apple","quantity":20}""", """
                {"productCode":"A001","productName":"Apple","quantity":30}""");
        List<Long> requestQuantities = List.of(10L, 20L, 30L);

        // when
        List<MvcResult> responses = completed(ConcurrencyRunner.run(3, i -> inbound(bodies.get(i))));

        // then
        assertThat(statusCount(responses, HttpStatus.OK)).isEqualTo(3);
        assertThat(responses.size() - statusCount(responses, HttpStatus.OK)).isZero();
        for (MvcResult response : responses) {
            expectSuccessBody(response, "A001", "Apple");
        }
        List<Long> quantities = sortedQuantities(responses);
        assertThat(quantities.getLast()).isEqualTo(160L);
        assertThat(steps(100L, quantities)).containsExactlyInAnyOrder(10L, 20L, 30L);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(160L);
        long successInbound = successQuantitySum(responses, requestQuantities);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(100L + successInbound - 0L);
        assertThat(successInbound).isEqualTo(10L + 20L + 30L);
    }

    @Test
    @DisplayName("[TC-2-12] 등록되지 않은 상품에 같은 상품명으로 10, 20, 30을 동시에 입고하면 상품을 하나만 만들고 재고가 60이 된다")
    void concurrentInboundCreatesSingleProduct() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        List<String> bodies = List.of("""
                {"productCode":"A001","productName":"Apple","quantity":10}""", """
                {"productCode":"A001","productName":"Apple","quantity":20}""", """
                {"productCode":"A001","productName":"Apple","quantity":30}""");
        List<Long> requestQuantities = List.of(10L, 20L, 30L);

        // when
        List<MvcResult> responses = completed(ConcurrencyRunner.run(3, i -> inbound(bodies.get(i))));

        // then
        assertThat(statusCount(responses, HttpStatus.OK)).isEqualTo(3);
        assertThat(responses.size() - statusCount(responses, HttpStatus.OK)).isZero();
        for (MvcResult response : responses) {
            expectSuccessBody(response, "A001", "Apple");
        }
        List<Long> quantities = sortedQuantities(responses);
        assertThat(quantities.getLast()).isEqualTo(60L);
        assertThat(steps(0L, quantities)).containsExactlyInAnyOrder(10L, 20L, 30L);
        assertThat(db.productNames("tenant-001", "A001")).containsExactly("Apple");
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(60L);
        long successInbound = successQuantitySum(responses, requestQuantities);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(0L + successInbound - 0L);
        assertThat(successInbound).isEqualTo(10L + 20L + 30L);
    }

    @Test
    @DisplayName("[TC-2-13] 등록되지 않은 상품에 다른 상품명으로 동시에 입고하면 한 건만 성공하고 나머지는 PRODUCT_NAME_MISMATCH로 거부한다")
    void concurrentInboundWithDifferentNamesAcceptsOnlyOne() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        List<String> bodies = List.of("""
                {"productCode":"A001","productName":"Apple","quantity":10}""", """
                {"productCode":"A001","productName":"Samsung","quantity":20}""");
        List<String> requestNames = List.of("Apple", "Samsung");
        List<Long> requestQuantities = List.of(10L, 20L);

        // when
        List<MvcResult> responses = completed(ConcurrencyRunner.run(2, i -> inbound(bodies.get(i))));

        // then
        assertThat(statusCount(responses, HttpStatus.OK)).isEqualTo(1);
        assertThat(statusCount(responses, HttpStatus.CONFLICT)).isEqualTo(1);
        int winner = indexOf(responses, HttpStatus.OK);
        int loser = indexOf(responses, HttpStatus.CONFLICT);
        String winnerName = requestNames.get(winner);
        long winnerQuantity = requestQuantities.get(winner);
        expectSuccessBody(responses.get(winner), "A001", winnerName);
        assertThat(quantity(responses.get(winner))).isEqualTo(winnerQuantity);
        assertThat((String) JsonPath.read(content(responses.get(loser)), "$.code"))
                .isEqualTo("PRODUCT_NAME_MISMATCH");
        assertThat(db.productNames("tenant-001", "A001")).containsExactly(winnerName);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(winnerQuantity);
        long successInbound = successQuantitySum(responses, requestQuantities);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(0L + successInbound - 0L);
    }

    private MvcResult inbound(String body) throws Exception {
        return mockMvc.perform(post(INBOUND_URL)
                        .header(TENANT_HEADER, "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private static List<MvcResult> completed(List<ConcurrencyRunner.Result<MvcResult>> results) {
        assertThat(ConcurrencyRunner.errors(results)).isEmpty();
        return results.stream().map(ConcurrencyRunner.Result::value).toList();
    }

    private static long statusCount(List<MvcResult> responses, HttpStatus status) {
        return responses.stream()
                .filter(response -> response.getResponse().getStatus() == status.value())
                .count();
    }

    private static int indexOf(List<MvcResult> responses, HttpStatus status) {
        return IntStream.range(0, responses.size())
                .filter(i -> responses.get(i).getResponse().getStatus() == status.value())
                .findFirst()
                .orElseThrow();
    }

    private static long successQuantitySum(List<MvcResult> responses, List<Long> requestQuantities) {
        return IntStream.range(0, responses.size())
                .filter(i -> responses.get(i).getResponse().getStatus() == HttpStatus.OK.value())
                .mapToLong(requestQuantities::get)
                .sum();
    }

    private static void expectSuccessBody(MvcResult response, String productCode, String productName) {
        String body = content(response);
        assertThat((String) JsonPath.read(body, "$.productCode")).isEqualTo(productCode);
        assertThat((String) JsonPath.read(body, "$.productName")).isEqualTo(productName);
        String updatedAt = JsonPath.read(body, "$.updatedAt");
        assertThat(OffsetDateTime.parse(updatedAt).getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }

    private static long quantity(MvcResult response) {
        Number quantity = JsonPath.read(content(response), "$.quantity");
        return quantity.longValue();
    }

    private static List<Long> sortedQuantities(List<MvcResult> responses) {
        return responses.stream().map(InboundConcurrencyTest::quantity).sorted().toList();
    }

    private static List<Long> steps(long initial, List<Long> sortedQuantities) {
        List<Long> steps = new ArrayList<>();
        long previous = initial;
        for (long quantity : sortedQuantities) {
            steps.add(quantity - previous);
            previous = quantity;
        }
        return steps;
    }

    private static String content(MvcResult response) {
        try {
            return response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}

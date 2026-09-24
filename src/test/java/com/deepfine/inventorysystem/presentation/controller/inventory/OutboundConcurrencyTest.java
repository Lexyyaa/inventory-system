package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.deepfine.inventorysystem.support.ConcurrencyRunner;
import com.deepfine.inventorysystem.support.IntegrationTest;
import com.deepfine.inventorysystem.support.InventoryTestDb;
import com.jayway.jsonpath.JsonPath;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
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
class OutboundConcurrencyTest {

    private static final String INBOUND_URL = "/api/v1/inventory/inbound";
    private static final String OUTBOUND_URL = "/api/v1/inventory/outbound";
    private static final String CURRENT_STOCK_URL = "/api/v1/inventory/{productCode}";

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
    @DisplayName("[TC-4-07] 재고 10에서 10개 출고 두 건이 동시에 오면 한 건만 성공하고 최종 재고는 0이다")
    void concurrentOutboundOfTwoAcceptsOnlyOne() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        String body = """
                {"productCode":"A001","quantity":10}""";
        List<Long> requestQuantities = Collections.nCopies(2, 10L);

        // when
        List<MvcResult> responses = completed(ConcurrencyRunner.run(2, i -> outbound(body)));

        // then
        assertThat(statusCount(responses, HttpStatus.OK)).isEqualTo(1);
        assertThat(statusCount(responses, HttpStatus.CONFLICT)).isEqualTo(1);
        assertThat(otherStatusCount(responses)).isZero();
        MvcResult success = responses.get(indexOf(responses, HttpStatus.OK));
        expectSuccessBody(success);
        assertThat(quantity(success)).isZero();
        MvcResult failure = responses.get(indexOf(responses, HttpStatus.CONFLICT));
        assertThat(code(failure)).isEqualTo("INSUFFICIENT_STOCK");
        long finalQuantity = currentStockQuantity();
        assertThat(finalQuantity).isZero();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(0L);
        long successOutbound = successQuantitySum(responses, requestQuantities);
        assertThat(successOutbound).isEqualTo(10L);
        assertThat(finalQuantity).isEqualTo(10L + 0L - successOutbound);
    }

    @Test
    @DisplayName("[TC-4-08] 재고 100에서 10개 출고 스무 건이 동시에 오면 열 건만 성공하고 최종 재고는 0이다")
    void concurrentOutboundOfTwentyAcceptsOnlyTen() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 100);
        String body = """
                {"productCode":"A001","quantity":10}""";
        List<Long> requestQuantities = Collections.nCopies(20, 10L);

        // when
        List<MvcResult> responses = completed(ConcurrencyRunner.run(20, i -> outbound(body)));

        // then
        assertThat(statusCount(responses, HttpStatus.OK)).isEqualTo(10);
        assertThat(statusCount(responses, HttpStatus.CONFLICT)).isEqualTo(10);
        assertThat(otherStatusCount(responses)).isZero();
        List<MvcResult> successes = withStatus(responses, HttpStatus.OK);
        for (MvcResult success : successes) {
            expectSuccessBody(success);
        }
        assertThat(successes.stream().map(OutboundConcurrencyTest::quantity).toList())
                .containsExactlyInAnyOrder(90L, 80L, 70L, 60L, 50L, 40L, 30L, 20L, 10L, 0L);
        assertThat(withStatus(responses, HttpStatus.CONFLICT).stream()
                        .map(OutboundConcurrencyTest::code)
                        .toList())
                .containsOnly("INSUFFICIENT_STOCK");
        long finalQuantity = currentStockQuantity();
        assertThat(finalQuantity).isZero();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(0L);
        long successOutbound = successQuantitySum(responses, requestQuantities);
        assertThat(successOutbound).isEqualTo(10L * 10L);
        assertThat(finalQuantity).isEqualTo(100L + 0L - successOutbound);
    }

    @Test
    @DisplayName("[TC-4-09] 재고 10에서 10 입고와 15 출고가 동시에 오면 출고 결과에 따라 최종 재고가 5 또는 20이 된다")
    void concurrentInboundAndOutboundKeepStockConsistent() throws Exception {
        // given
        db.insertProduct("tenant-001", "A001", "Apple", 10);
        String inboundBody = """
                {"productCode":"A001","productName":"Apple","quantity":10}""";
        String outboundBody = """
                {"productCode":"A001","quantity":15}""";

        // when
        List<MvcResult> responses =
                completed(ConcurrencyRunner.run(2, i -> i == 0 ? inbound(inboundBody) : outbound(outboundBody)));

        // then
        MvcResult inbound = responses.get(0);
        MvcResult outbound = responses.get(1);
        assertThat(status(inbound)).isEqualTo(HttpStatus.OK.value());
        expectSuccessBody(inbound);
        assertThat(quantity(inbound)).isEqualTo(20L);
        assertThat(status(outbound)).isIn(HttpStatus.OK.value(), HttpStatus.CONFLICT.value());
        assertThat(otherStatusCount(responses)).isZero();
        boolean outboundSucceeded = status(outbound) == HttpStatus.OK.value();
        if (outboundSucceeded) {
            expectSuccessBody(outbound);
            assertThat(quantity(outbound)).isEqualTo(5L);
        } else {
            assertThat(code(outbound)).isEqualTo("INSUFFICIENT_STOCK");
        }
        long finalQuantity = currentStockQuantity();
        assertThat(finalQuantity).isEqualTo(outboundSucceeded ? 5L : 20L);
        assertThat(db.inventoryQuantities("tenant-001", "A001")).containsExactly(finalQuantity);
        long successInbound = status(inbound) == HttpStatus.OK.value() ? 10L : 0L;
        long successOutbound = outboundSucceeded ? 15L : 0L;
        assertThat(finalQuantity).isEqualTo(10L + successInbound - successOutbound);
    }

    private MvcResult inbound(String body) throws Exception {
        return mockMvc.perform(post(INBOUND_URL)
                        .header(TENANT_HEADER, "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private MvcResult outbound(String body) throws Exception {
        return mockMvc.perform(post(OUTBOUND_URL)
                        .header(TENANT_HEADER, "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();
    }

    private long currentStockQuantity() throws Exception {
        MvcResult response = mockMvc.perform(get(CURRENT_STOCK_URL, "A001").header(TENANT_HEADER, "tenant-001"))
                .andReturn();
        assertThat(status(response)).isEqualTo(HttpStatus.OK.value());
        return quantity(response);
    }

    private static List<MvcResult> completed(List<ConcurrencyRunner.Result<MvcResult>> results) {
        assertThat(ConcurrencyRunner.errors(results)).isEmpty();
        return results.stream().map(ConcurrencyRunner.Result::value).toList();
    }

    private static int status(MvcResult response) {
        return response.getResponse().getStatus();
    }

    private static long statusCount(List<MvcResult> responses, HttpStatus status) {
        return withStatus(responses, status).size();
    }

    private static long otherStatusCount(List<MvcResult> responses) {
        return responses.size() - statusCount(responses, HttpStatus.OK) - statusCount(responses, HttpStatus.CONFLICT);
    }

    private static List<MvcResult> withStatus(List<MvcResult> responses, HttpStatus status) {
        return responses.stream()
                .filter(response -> status(response) == status.value())
                .toList();
    }

    private static int indexOf(List<MvcResult> responses, HttpStatus status) {
        return IntStream.range(0, responses.size())
                .filter(i -> status(responses.get(i)) == status.value())
                .findFirst()
                .orElseThrow();
    }

    private static long successQuantitySum(List<MvcResult> responses, List<Long> requestQuantities) {
        return IntStream.range(0, responses.size())
                .filter(i -> status(responses.get(i)) == HttpStatus.OK.value())
                .mapToLong(requestQuantities::get)
                .sum();
    }

    private static void expectSuccessBody(MvcResult response) {
        String body = content(response);
        assertThat((String) JsonPath.read(body, "$.productCode")).isEqualTo("A001");
        assertThat((String) JsonPath.read(body, "$.productName")).isEqualTo("Apple");
        String updatedAt = JsonPath.read(body, "$.updatedAt");
        assertThat(OffsetDateTime.parse(updatedAt).getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    }

    private static long quantity(MvcResult response) {
        Number quantity = JsonPath.read(content(response), "$.quantity");
        return quantity.longValue();
    }

    private static String code(MvcResult response) {
        return JsonPath.read(content(response), "$.code");
    }

    private static String content(MvcResult response) {
        try {
            return response.getResponse().getContentAsString(StandardCharsets.UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}

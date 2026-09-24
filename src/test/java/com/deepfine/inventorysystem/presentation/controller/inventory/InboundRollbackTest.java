package com.deepfine.inventorysystem.presentation.controller.inventory;

import static com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deepfine.inventorysystem.infrastructure.persistence.inventory.InventoryRepositoryImpl;
import com.deepfine.inventorysystem.support.IntegrationTest;
import com.deepfine.inventorysystem.support.InventoryTestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class InboundRollbackTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoSpyBean
    InventoryRepositoryImpl inventoryRepository;

    InventoryTestDb db;

    @BeforeEach
    void setUp() {
        db = new InventoryTestDb(jdbcTemplate);
        db.clear();
    }

    @Test
    @DisplayName("[TC-2-10] 신규 상품 입고 중 재고 반영 단계에서 실패하면 INTERNAL_SERVER_ERROR로 응답하고 상품과 재고를 모두 남기지 않는다")
    void rollsBackProductWhenInventoryUpdateFails() throws Exception {
        // given
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        doThrow(new RuntimeException("injected")).when(inventoryRepository).increase(anyLong(), anyLong());

        // when
        ResultActions result = mockMvc.perform(post("/api/v1/inventory/inbound")
                .header(TENANT_HEADER, "tenant-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"productCode":"A001","productName":"Apple","quantity":10}"""));

        // then
        result.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value(not(containsString("injected"))));
        ArgumentCaptor<Long> productId = ArgumentCaptor.forClass(Long.class);
        verify(inventoryRepository).increase(productId.capture(), eq(10L));
        assertThat(productId.getValue()).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM product WHERE id = ?", Long.class, productId.getValue()))
                .isZero();
        assertThat(db.productNames("tenant-001", "A001")).isEmpty();
        assertThat(db.inventoryQuantities("tenant-001", "A001")).isEmpty();
    }
}

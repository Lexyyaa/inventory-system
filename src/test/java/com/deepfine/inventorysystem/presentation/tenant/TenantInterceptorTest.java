package com.deepfine.inventorysystem.presentation.tenant;

import static com.deepfine.inventorysystem.presentation.tenant.TenantInterceptor.TENANT_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deepfine.inventorysystem.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@IntegrationTest
class TenantInterceptorTest {

    private static final String TEST_TENANT_URL = "/api/v1/test/tenant";
    private static final String INVALID_TENANT_MESSAGE = "Tenant 정보가 없거나 등록되지 않았습니다.";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("[TC-1-01] X-Tenant-Id가 없거나 빈 문자열이거나 공백뿐이면 400 INVALID_TENANT로 거부한다")
    void rejectsMissingOrBlankTenantHeader() throws Exception {
        // given
        assertThat(tenantCodes()).contains("tenant-001", "tenant-002");
        Long tenant001Id = tenantId("tenant-001");

        // when
        ResultActions precheck = mockMvc.perform(get(TEST_TENANT_URL).header(TENANT_HEADER, "tenant-001"));
        ResultActions withoutHeader = mockMvc.perform(get(TEST_TENANT_URL));
        ResultActions emptyHeader = mockMvc.perform(get(TEST_TENANT_URL).header(TENANT_HEADER, ""));
        ResultActions blankHeader = mockMvc.perform(get(TEST_TENANT_URL).header(TENANT_HEADER, "   "));

        // then
        precheck.andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantCode").value("tenant-001"))
                .andExpect(jsonPath("$.tenantId").value(tenant001Id));
        expectInvalidTenant(withoutHeader);
        expectInvalidTenant(emptyHeader);
        expectInvalidTenant(blankHeader);
    }

    @Test
    @DisplayName("[TC-1-02] 등록되지 않은 업체 코드로 요청하면 100자를 넘는 코드여도 400 INVALID_TENANT로 거부한다")
    void rejectsUnregisteredTenantCode() throws Exception {
        // given
        assertThat(tenantCodes()).containsExactlyInAnyOrder("tenant-001", "tenant-002");

        // when
        ResultActions unregistered = mockMvc.perform(get(TEST_TENANT_URL).header(TENANT_HEADER, "tenant-999"));
        ResultActions overLength = mockMvc.perform(get(TEST_TENANT_URL).header(TENANT_HEADER, "t".repeat(101)));

        // then
        expectInvalidTenant(unregistered);
        expectInvalidTenant(overLength);
        assertThat(tenantCodes()).hasSize(2).containsExactlyInAnyOrder("tenant-001", "tenant-002");
    }

    private static void expectInvalidTenant(ResultActions result) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TENANT"))
                .andExpect(jsonPath("$.message").value(INVALID_TENANT_MESSAGE));
    }

    private List<String> tenantCodes() {
        return jdbcTemplate.queryForList("SELECT code FROM tenant", String.class);
    }

    private Long tenantId(String code) {
        return jdbcTemplate.queryForObject("SELECT id FROM tenant WHERE code = ?", Long.class, code);
    }
}

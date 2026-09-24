package com.deepfine.inventorysystem.support.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info().title("재고 관리 시스템 API").description("""
                                업체별 상품 입고 · 출고 · 현재 재고 조회 API (MVP).
                                모든 /api/v1 요청은 X-Tenant-Id 헤더로 업체를 식별한다.
                                재고 변경은 PostgreSQL 원자 SQL로 처리해 동시 요청에서도 재고가 음수가 되거나 누락되지 않는다.""").version("v1"));
    }
}

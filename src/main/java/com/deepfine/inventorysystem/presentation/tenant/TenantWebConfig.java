package com.deepfine.inventorysystem.presentation.tenant;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 업체 확인 인터셉터와 {@code @TenantId} 리졸버를 등록한다.
 * presentation 빈을 참조하므로 support/config가 아니라 여기에 둔다 (support → presentation 의존 금지).
 */
@Configuration
@RequiredArgsConstructor
public class TenantWebConfig implements WebMvcConfigurer {

    private static final String API_PATH_PATTERN = "/api/v1/**";

    private final TenantInterceptor tenantInterceptor;
    private final TenantIdArgumentResolver tenantIdArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns(API_PATH_PATTERN);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(tenantIdArgumentResolver);
    }
}

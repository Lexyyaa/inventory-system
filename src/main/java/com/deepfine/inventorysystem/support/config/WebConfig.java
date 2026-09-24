package com.deepfine.inventorysystem.support.config;

import static org.springframework.web.method.HandlerTypePredicate.forBasePackage;

import com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    public static final String API_PREFIX = "/api/v1";
    private static final String PRESENTATION_PACKAGE = "com.deepfine.inventorysystem.presentation";

    private final TenantInterceptor tenantInterceptor;

    /**
     * API 경로 접두사 <br>
     * - presentation 패키지의 컨트롤러에만 붙인다 <br>
     * - Swagger 같은 라이브러리 경로에는 붙지 않게 하려는 것이다 <br>
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, forBasePackage(PRESENTATION_PACKAGE));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor).addPathPatterns(API_PREFIX + "/**");
    }
}

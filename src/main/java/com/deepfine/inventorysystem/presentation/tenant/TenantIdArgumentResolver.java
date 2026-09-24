package com.deepfine.inventorysystem.presentation.tenant;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@link TenantInterceptor}가 request attribute에 넣어 둔 업체 id를 {@code @TenantId Long} 파라미터로 넘긴다.
 * 업체 확인은 인터셉터에서 끝났으므로 여기서는 꺼내기만 한다.
 */
@Component
public class TenantIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(TenantId.class) && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Long resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory) {
        Object tenantId =
                webRequest.getAttribute(TenantInterceptor.TENANT_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (tenantId == null) {
            // 인터셉터가 걸리지 않는 경로(/api/v1/** 밖)에서 @TenantId를 쓴 코드 결함이다 → 500
            throw new IllegalStateException("업체 확인 인터셉터를 거치지 않은 요청에서 @TenantId를 사용했습니다.");
        }
        return (Long) tenantId;
    }
}

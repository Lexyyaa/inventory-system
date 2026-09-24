package com.deepfine.inventorysystem.presentation.tenant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link TenantInterceptor}가 확인한 업체 id를 컨트롤러 파라미터({@code @TenantId Long tenantId})로 받는다.
 * {@code /api/v1/**} 경로의 컨트롤러에서만 쓴다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantId {}

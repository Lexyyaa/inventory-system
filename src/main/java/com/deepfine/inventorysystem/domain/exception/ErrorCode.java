package com.deepfine.inventorysystem.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "필수 요청 정보가 누락되었습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 업체
    INVALID_TENANT(HttpStatus.BAD_REQUEST, "Tenant 정보가 없거나 등록되지 않았습니다."),

    // 상품 · 재고
    INVALID_QUANTITY(HttpStatus.BAD_REQUEST, "입고 수량이 허용 범위를 벗어났습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    PRODUCT_NAME_MISMATCH(HttpStatus.CONFLICT, "동일한 상품코드에 등록된 상품명과 일치하지 않습니다."),
    INSUFFICIENT_STOCK(HttpStatus.CONFLICT, "출고 수량이 현재 재고보다 많습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}

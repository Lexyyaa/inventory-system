package com.deepfine.inventorysystem.support.exception;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;

/**
 * 에러 응답 본문. { "code": "...", "message": "..." } (docs/design/04-api-spec.md §2 "공통 오류 응답")
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}

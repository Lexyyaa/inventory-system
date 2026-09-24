package com.deepfine.inventorysystem.support.exception;

import com.deepfine.inventorysystem.domain.exception.ErrorCode;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message);
    }
}

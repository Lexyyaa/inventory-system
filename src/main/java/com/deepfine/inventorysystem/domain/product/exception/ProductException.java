package com.deepfine.inventorysystem.domain.product.exception;

import com.deepfine.inventorysystem.domain.exception.BusinessException;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;

public class ProductException extends BusinessException {

    public ProductException(ErrorCode errorCode) {
        super(errorCode);
    }
}

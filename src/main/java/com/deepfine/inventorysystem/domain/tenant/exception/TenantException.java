package com.deepfine.inventorysystem.domain.tenant.exception;

import com.deepfine.inventorysystem.domain.exception.BusinessException;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;

public class TenantException extends BusinessException {

    public TenantException(ErrorCode errorCode) {
        super(errorCode);
    }
}

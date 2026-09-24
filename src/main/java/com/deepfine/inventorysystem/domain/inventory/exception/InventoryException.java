package com.deepfine.inventorysystem.domain.inventory.exception;

import com.deepfine.inventorysystem.domain.exception.BusinessException;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;

public class InventoryException extends BusinessException {

    public InventoryException(ErrorCode errorCode) {
        super(errorCode);
    }
}

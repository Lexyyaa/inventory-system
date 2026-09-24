package com.deepfine.inventorysystem.application.inventory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryCommand {

    /** 입고 요청. 형식 · 필수값 검증은 요청 단계에서 끝났고, 수량 범위는 서비스가 검사한다. */
    public record Inbound(String productCode, String productName, long quantity) {

        public static Inbound of(String productCode, String productName, long quantity) {
            return new Inbound(productCode, productName, quantity);
        }
    }
}

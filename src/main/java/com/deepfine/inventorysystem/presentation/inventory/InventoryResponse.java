package com.deepfine.inventorysystem.presentation.inventory;

import com.deepfine.inventorysystem.application.inventory.InventoryInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryResponse {

    /** 입고 응답 (04 §3). 내부 식별자는 담지 않는다. */
    @Schema(name = "InboundResponse", description = "입고 결과")
    public record Inbound(
            @Schema(description = "상품 코드", example = "A001") String productCode,

            @Schema(description = "저장된 상품명", example = "Apple")
            String productName,

            @Schema(description = "이 요청의 입고가 반영된 직후의 재고 수량", example = "110")
            long quantity,

            @Schema(description = "재고가 마지막으로 변경된 시각 (+09:00)", example = "2026-09-24T22:10:00+09:00")
            OffsetDateTime updatedAt) {

        public static Inbound from(InventoryInfo.Inbound info) {
            return new Inbound(info.productCode(), info.productName(), info.quantity(), info.updatedAt());
        }
    }
}

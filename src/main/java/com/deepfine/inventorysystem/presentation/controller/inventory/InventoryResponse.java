package com.deepfine.inventorysystem.presentation.controller.inventory;

import com.deepfine.inventorysystem.application.inventory.InventoryInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryResponse {

    private static final ZoneId RESPONSE_ZONE = ZoneId.of("Asia/Seoul");

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
            return new Inbound(
                    info.productCode(),
                    info.productName(),
                    info.quantity(),
                    info.updatedAt().atZone(RESPONSE_ZONE).toOffsetDateTime());
        }
    }

    @Schema(name = "OutboundResponse", description = "출고 결과")
    public record Outbound(
            @Schema(description = "상품 코드", example = "A001") String productCode,

            @Schema(description = "상품명", example = "Apple") String productName,

            @Schema(description = "이 요청의 출고가 반영된 직후의 재고 수량", example = "90")
            long quantity,

            @Schema(description = "재고가 마지막으로 변경된 시각 (+09:00)", example = "2026-09-24T22:10:00+09:00")
            OffsetDateTime updatedAt) {

        public static Outbound from(InventoryInfo.Outbound info) {
            return new Outbound(
                    info.productCode(),
                    info.productName(),
                    info.quantity(),
                    info.updatedAt().atZone(RESPONSE_ZONE).toOffsetDateTime());
        }
    }

    @Schema(name = "CurrentStockResponse", description = "현재 재고")
    public record CurrentStock(
            @Schema(description = "상품 코드", example = "A001") String productCode,

            @Schema(description = "상품명", example = "Apple") String productName,

            @Schema(description = "조회 시점에 커밋되어 있던 재고 수량", example = "100")
            long quantity,

            @Schema(description = "재고가 마지막으로 변경된 시각 (+09:00)", example = "2026-09-24T22:10:00+09:00")
            OffsetDateTime updatedAt) {

        public static CurrentStock from(InventoryInfo.CurrentStock info) {
            return new CurrentStock(
                    info.productCode(),
                    info.productName(),
                    info.quantity(),
                    info.updatedAt().atZone(RESPONSE_ZONE).toOffsetDateTime());
        }
    }
}

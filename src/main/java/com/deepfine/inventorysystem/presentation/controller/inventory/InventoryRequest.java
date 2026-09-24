package com.deepfine.inventorysystem.presentation.controller.inventory;

import com.deepfine.inventorysystem.application.inventory.InventoryCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.CodePointLength;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryRequest {

    @Schema(name = "InboundRequest", description = "입고 요청")
    public record Inbound(
            @Schema(
                    description = "상품 코드 (영문 · 숫자 · _ · -, 1~100자, 대소문자 구분)",
                    example = "A001",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank
            @Size(max = 100)
            @Pattern(regexp = "[A-Za-z0-9_-]+")
            String productCode,

            @Schema(
                    description = "상품명 (1~255자). 기존 상품이면 등록된 상품명과 같아야 한다",
                    example = "Apple",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank
            @CodePointLength(max = 255)
            @Pattern(regexp = "[^\\u0000\\uD800-\\uDFFF]*")
            String productName,

            @Schema(
                    description = "입고 수량 (1 ~ 1,000,000,000 정수)",
                    example = "10",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull
            Long quantity) {

        public InventoryCommand.Inbound toCommand(Long tenantId) {
            return new InventoryCommand.Inbound(tenantId, productCode, productName, quantity);
        }
    }
}

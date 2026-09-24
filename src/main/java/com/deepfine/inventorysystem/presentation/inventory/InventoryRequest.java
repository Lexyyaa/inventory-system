package com.deepfine.inventorysystem.presentation.inventory;

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

    /**
     * 입고 요청 본문 (04 §3). 필수값 · 길이 · 허용 문자를 벗어나면 400 INVALID_REQUEST다.
     * 수량 범위(1 ~ 상한)는 설정값이라 여기서 검사하지 않고 서비스가 INVALID_QUANTITY로 검사한다.
     * 상품명의 NUL 문자와 짝이 없는 서로게이트는 PostgreSQL(UTF-8) 문자열에 그대로 저장할 수 없으므로 여기서 거부한다.
     * NUL은 DB 오류로 500이 되고, 짝 없는 서로게이트는 다른 문자로 바뀌어 저장돼 상품명 비교가 어긋난다.
     * 상품명 길이는 DB {@code VARCHAR(255)}와 같게 문자(코드포인트) 단위로 센다. {@code @Size}는 UTF-16 단위라 이모지 같은
     * 보충 평면 문자를 2자로 세어 DB 범위 안의 상품명을 거부한다. 상품코드는 허용 문자가 ASCII라 {@code @Size}로 충분하다.
     */
    @Schema(name = "InboundRequest", description = "입고 요청")
    public record Inbound(
            @Schema(
                    description = "상품 코드. 영문 대소문자 · 숫자 · _ · - 로 이루어진 1~100자, 대소문자 구분, 업체 안에서 유일",
                    example = "A001",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank
            @Size(max = 100)
            @Pattern(regexp = "[A-Za-z0-9_-]+")
            String productCode,

            @Schema(
                    description = "상품명. 1~255자, 공백만으로 이루어질 수 없다. 기존 상품이면 등록된 상품명과 정확히 같아야 한다",
                    example = "Apple",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank
            @CodePointLength(max = 255)
            @Pattern(regexp = "[^\\u0000\\uD800-\\uDFFF]*")
            String productName,

            @Schema(
                    description = "입고 수량. 1 이상 1,000,000,000 이하의 JSON 정수",
                    example = "10",
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull
            Long quantity) {}
}

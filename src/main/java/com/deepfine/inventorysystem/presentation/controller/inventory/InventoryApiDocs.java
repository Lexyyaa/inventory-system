package com.deepfine.inventorysystem.presentation.controller.inventory;

import com.deepfine.inventorysystem.presentation.interceptor.TenantInterceptor;
import com.deepfine.inventorysystem.support.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "재고", description = "상품 입고 · 출고 · 현재 재고 조회")
public interface InventoryApiDocs {

    @Operation(summary = "상품 입고", description = """
            요청 업체의 상품 재고에 입고 수량을 더한다.

            - 요청 업체에 없는 상품코드면 요청 업체의 상품으로 만든 뒤 입고한다. 다른 업체에 같은 상품코드가 있어도 신규 상품이다.
            - 이미 있는 상품이면 상품명이 대소문자 · 공백까지 등록된 상품명과 같아야 한다. 입고로 상품명을 바꾸지 않는다.
            - 응답의 quantity는 이 요청이 반영된 직후의 재고 수량이다. 동시에 처리된 요청들은 서로 다른 값을 받을 수 있다.
            - 오류가 겹치면 업체 확인 → 요청 형식 → 필수값 · 길이 → 수량 범위 → 상품 상태 순으로 먼저 걸린 하나만 돌려준다.
            """)
    @Parameter(
            name = TenantInterceptor.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "업체 코드. 없거나 비어 있거나 등록되지 않았으면 400 INVALID_TENANT",
            example = "tenant-001")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "입고 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "INVALID_TENANT · INVALID_REQUEST · INVALID_QUANTITY",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = {
                                    @ExampleObject(
                                            name = "INVALID_TENANT",
                                            summary = "업체 정보 누락 또는 미등록",
                                            value =
                                                    "{\"code\": \"INVALID_TENANT\", \"message\": \"Tenant 정보가 없거나 등록되지 않았습니다.\"}"),
                                    @ExampleObject(
                                            name = "INVALID_REQUEST (형식)",
                                            summary = "깨진 JSON · 정수가 아닌 수량",
                                            value =
                                                    "{\"code\": \"INVALID_REQUEST\", \"message\": \"요청 형식이 올바르지 않습니다.\"}"),
                                    @ExampleObject(
                                            name = "INVALID_REQUEST (누락)",
                                            summary = "필수값 누락 · 빈 값 · 길이 · 허용 문자 위반",
                                            value =
                                                    "{\"code\": \"INVALID_REQUEST\", \"message\": \"필수 요청 정보가 누락되었습니다.\"}"),
                                    @ExampleObject(
                                            name = "INVALID_QUANTITY",
                                            summary = "수량이 1 미만이거나 상한 초과",
                                            value =
                                                    "{\"code\": \"INVALID_QUANTITY\", \"message\": \"입고 수량이 허용 범위를 벗어났습니다.\"}")
                                })),
        @ApiResponse(
                responseCode = "409",
                description = "PRODUCT_NAME_MISMATCH",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "PRODUCT_NAME_MISMATCH",
                                                summary = "기존 상품명과 불일치",
                                                value =
                                                        "{\"code\": \"PRODUCT_NAME_MISMATCH\", \"message\": \"동일한 상품코드에 등록된 상품명과 일치하지 않습니다.\"}"))),
        @ApiResponse(
                responseCode = "500",
                description = "INTERNAL_SERVER_ERROR. 요청은 반영되지 않는다",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "INTERNAL_SERVER_ERROR",
                                                value =
                                                        "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"서버 오류가 발생했습니다.\"}")))
    })
    InventoryResponse.Inbound inbound(@Parameter(hidden = true) Long tenantId, InventoryRequest.Inbound request);

    @Operation(summary = "상품 출고", description = """
            요청 업체의 상품 재고에서 출고 수량을 뺀다.

            - 상품코드는 요청 업체 안에서 찾는다. 다른 업체에만 있는 상품코드는 없는 상품으로 보고 404를 돌려준다. 출고로 상품을 만들지 않는다.
            - 현재 재고보다 많은 수량이면 409를 돌려주고 재고를 바꾸지 않는다. 가능한 수량만 나누어 출고하지 않는다.
            - 응답의 quantity는 이 요청의 출고가 반영된 직후의 재고 수량이다. 동시에 처리된 요청들은 서로 다른 값을 받을 수 있다.
            - 오류가 겹치면 업체 확인 → 요청 형식 → 필수값 · 길이 → 수량 범위 → 상품 존재 → 재고 상태 순으로 먼저 걸린 하나만 돌려준다.
            """)
    @Parameter(
            name = TenantInterceptor.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "업체 코드. 없거나 비어 있거나 등록되지 않았으면 400 INVALID_TENANT",
            example = "tenant-001")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "출고 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "INVALID_TENANT · INVALID_REQUEST · INVALID_QUANTITY",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples = {
                                    @ExampleObject(
                                            name = "INVALID_TENANT",
                                            summary = "업체 정보 누락 또는 미등록",
                                            value =
                                                    "{\"code\": \"INVALID_TENANT\", \"message\": \"Tenant 정보가 없거나 등록되지 않았습니다.\"}"),
                                    @ExampleObject(
                                            name = "INVALID_REQUEST (형식)",
                                            summary = "깨진 JSON · 정수가 아닌 수량",
                                            value =
                                                    "{\"code\": \"INVALID_REQUEST\", \"message\": \"요청 형식이 올바르지 않습니다.\"}"),
                                    @ExampleObject(
                                            name = "INVALID_REQUEST (누락)",
                                            summary = "필수값 누락 · 빈 값 · 길이 · 허용 문자 위반",
                                            value =
                                                    "{\"code\": \"INVALID_REQUEST\", \"message\": \"필수 요청 정보가 누락되었습니다.\"}"),
                                    @ExampleObject(
                                            name = "INVALID_QUANTITY",
                                            summary = "수량이 1 미만이거나 상한 초과",
                                            value =
                                                    "{\"code\": \"INVALID_QUANTITY\", \"message\": \"출고 수량이 허용 범위를 벗어났습니다.\"}")
                                })),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "PRODUCT_NOT_FOUND",
                                                summary = "요청 업체에 해당 상품코드의 상품이 없음",
                                                value =
                                                        "{\"code\": \"PRODUCT_NOT_FOUND\", \"message\": \"상품을 찾을 수 없습니다.\"}"))),
        @ApiResponse(
                responseCode = "409",
                description = "INSUFFICIENT_STOCK",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "INSUFFICIENT_STOCK",
                                                summary = "출고 수량이 현재 재고보다 많음",
                                                value =
                                                        "{\"code\": \"INSUFFICIENT_STOCK\", \"message\": \"출고 수량이 현재 재고보다 많습니다.\"}"))),
        @ApiResponse(
                responseCode = "500",
                description = "INTERNAL_SERVER_ERROR. 요청은 반영되지 않는다",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "INTERNAL_SERVER_ERROR",
                                                value =
                                                        "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"서버 오류가 발생했습니다.\"}")))
    })
    InventoryResponse.Outbound outbound(@Parameter(hidden = true) Long tenantId, InventoryRequest.Outbound request);

    @Operation(summary = "현재 재고 조회", description = """
            요청 업체에 속한 상품의 현재 재고를 조회한다.

            - 상품코드는 요청 업체 안에서 찾는다. 다른 업체에만 있는 상품코드는 없는 상품으로 보고 404를 돌려준다.
            - 경로의 상품코드는 형식을 검증하지 않는다. 허용 문자 · 길이를 벗어난 코드는 등록될 수 없어 404다.
            - 응답의 quantity는 조회 시점에 커밋되어 있던 재고 수량이다. 조회는 입고 · 출고를 막지 않아 응답 뒤에 재고가 바뀔 수 있다.
            """)
    @Parameter(
            name = TenantInterceptor.TENANT_HEADER,
            in = ParameterIn.HEADER,
            required = true,
            description = "업체 코드. 없거나 비어 있거나 등록되지 않았으면 400 INVALID_TENANT",
            example = "tenant-001")
    @Parameter(
            name = "productCode",
            in = ParameterIn.PATH,
            required = true,
            description = "상품 코드 (대소문자 구분). 요청 업체에 없으면 404 PRODUCT_NOT_FOUND",
            example = "A001")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(
                responseCode = "400",
                description = "INVALID_TENANT",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "INVALID_TENANT",
                                                summary = "업체 정보 누락 또는 미등록",
                                                value =
                                                        "{\"code\": \"INVALID_TENANT\", \"message\": \"Tenant 정보가 없거나 등록되지 않았습니다.\"}"))),
        @ApiResponse(
                responseCode = "404",
                description = "PRODUCT_NOT_FOUND",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "PRODUCT_NOT_FOUND",
                                                summary = "요청 업체에 해당 상품코드의 상품이 없음",
                                                value =
                                                        "{\"code\": \"PRODUCT_NOT_FOUND\", \"message\": \"상품을 찾을 수 없습니다.\"}"))),
        @ApiResponse(
                responseCode = "500",
                description = "INTERNAL_SERVER_ERROR",
                content =
                        @Content(
                                schema = @Schema(implementation = ErrorResponse.class),
                                examples =
                                        @ExampleObject(
                                                name = "INTERNAL_SERVER_ERROR",
                                                value =
                                                        "{\"code\": \"INTERNAL_SERVER_ERROR\", \"message\": \"서버 오류가 발생했습니다.\"}")))
    })
    InventoryResponse.CurrentStock getCurrentStock(@Parameter(hidden = true) Long tenantId, String productCode);
}

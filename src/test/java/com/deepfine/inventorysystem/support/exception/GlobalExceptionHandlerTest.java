package com.deepfine.inventorysystem.support.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deepfine.inventorysystem.domain.exception.BusinessException;
import com.deepfine.inventorysystem.domain.exception.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예외 → 응답 매핑 검증. DB 없이 standalone MockMvc로 돈다.
 */
class GlobalExceptionHandlerTest {

    // 04 §3 · §4 "오류 응답"의 INVALID_REQUEST 문구
    private static final String MISSING_MESSAGE = "필수 요청 정보가 누락되었습니다.";
    private static final String MALFORMED_MESSAGE = "요청 형식이 올바르지 않습니다.";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("BusinessException은 ErrorCode의 상태와 code, 넘긴 detail 문구로 응답한다")
    void businessException() throws Exception {
        mockMvc.perform(get("/samples/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("샘플이 없습니다."));
    }

    @Test
    @DisplayName("detail 없이 던진 BusinessException은 ErrorCode의 04 문구로 응답한다")
    void businessExceptionDefaultMessage() throws Exception {
        mockMvc.perform(get("/samples/tenant"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TENANT"))
                .andExpect(jsonPath("$.message").value("Tenant 정보가 없거나 등록되지 않았습니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    @DisplayName("요청 본문 필드 검증에 실패하면 400 INVALID_REQUEST와 필수 요청 정보 누락 문구로 응답한다")
    void invalidField() throws Exception {
        mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": " ", "items": [{"amount": 1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(MISSING_MESSAGE));
    }

    @Test
    @DisplayName("리스트 요소의 필드 검증도 수행되어 음수는 500이 아니라 400 INVALID_REQUEST로 응답한다")
    void invalidListElement() throws Exception {
        mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON).content("""
                                {"name": "a", "items": [{"amount": -1}]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(MISSING_MESSAGE));
    }

    @Test
    @DisplayName("본문 JSON 형식이 깨지면 400 INVALID_REQUEST와 요청 형식 오류 문구로 응답한다")
    void malformedJson() throws Exception {
        mockMvc.perform(post("/samples").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(MALFORMED_MESSAGE));
    }

    @Test
    @DisplayName("필수 헤더가 없으면 400 INVALID_REQUEST와 필수 요청 정보 누락 문구로 응답한다")
    void missingHeader() throws Exception {
        mockMvc.perform(get("/samples/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(MISSING_MESSAGE));
    }

    @Test
    @DisplayName("경로 변수 타입이 맞지 않으면 400 INVALID_REQUEST와 요청 형식 오류 문구로 응답한다")
    void typeMismatch() throws Exception {
        mockMvc.perform(get("/samples/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(MALFORMED_MESSAGE));
    }

    @Test
    @DisplayName("매핑되지 않은 경로는 404 RESOURCE_NOT_FOUND로 응답한다")
    void noHandler() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("@Validated 파라미터 제약 위반(ConstraintViolationException)도 400 INVALID_REQUEST로 응답한다")
    void constraintViolation() throws Exception {
        mockMvc.perform(get("/samples/constraint"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value(MISSING_MESSAGE));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405 METHOD_NOT_ALLOWED로 응답한다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/samples/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 INTERNAL_SERVER_ERROR로 응답하고 내부 메시지를 노출하지 않는다")
    void unexpected() throws Exception {
        mockMvc.perform(get("/samples/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다."));
    }

    @RestController
    static class SampleController {

        @GetMapping("/samples/business")
        void business() {
            throw new SampleException(ErrorCode.PRODUCT_NOT_FOUND, "샘플이 없습니다.");
        }

        @GetMapping("/samples/tenant")
        void tenant() {
            throw new SampleException(ErrorCode.INVALID_TENANT);
        }

        @PostMapping("/samples")
        void create(@RequestBody @Valid SampleRequest request) {}

        @GetMapping("/samples/header")
        void header(@RequestHeader("X-Sample-Id") String sampleId) {}

        @GetMapping("/samples/{id}")
        void byId(@PathVariable Long id) {}

        @GetMapping("/samples/constraint")
        void constraint() {
            throw new ConstraintViolationException("위반", Set.of());
        }

        @GetMapping("/samples/unexpected")
        void unexpected() {
            throw new IllegalStateException("내부 구현 정보");
        }
    }

    record SampleRequest(
            @NotBlank String name, @NotEmpty @Valid List<Item> items) {
        record Item(@NotNull @PositiveOrZero Long amount) {}
    }

    static class SampleException extends BusinessException {
        SampleException(ErrorCode errorCode) {
            super(errorCode);
        }

        SampleException(ErrorCode errorCode, String detail) {
            super(errorCode, detail);
        }
    }
}

package com.deepfine.inventorysystem.support.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("매핑되지 않은 경로는 404 RESOURCE_NOT_FOUND로 응답한다")
    void noHandler() throws Exception {
        mockMvc.perform(get("/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("지원하지 않는 메서드는 405 METHOD_NOT_ALLOWED로 응답한다")
    void methodNotAllowed() throws Exception {
        mockMvc.perform(post("/samples"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @RestController
    static class SampleController {

        @GetMapping("/samples")
        void sample() {}
    }
}

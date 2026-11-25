package com.datn.pc_store.common;

import com.datn.pc_store.common.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.core.TypeInformation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @RestController
    static class StubController {
        @GetMapping("/test/bad-sort")
        public void badSort() {
            throw new PropertyReferenceException("invalidField", TypeInformation.OBJECT, List.of());
        }

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new IllegalStateException("Trạng thái không hợp lệ");
        }

        @GetMapping("/test/forbidden")
        public void forbidden() {
            throw new AccessDeniedException("Từ chối truy cập");
        }

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new NoSuchElementException("Không tồn tại");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PropertyReferenceException trả về HTTP 400")
    void propertyReferenceReturns400() throws Exception {
        mockMvc.perform(get("/test/bad-sort"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("IllegalStateException trả về HTTP 409")
    void illegalStateReturns409() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("AccessDeniedException trả về HTTP 403")
    void accessDeniedReturns403() throws Exception {
        mockMvc.perform(get("/test/forbidden"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("NoSuchElementException trả về HTTP 404")
    void noSuchElementReturns404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound());
    }
}

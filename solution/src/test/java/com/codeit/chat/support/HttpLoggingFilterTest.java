package com.codeit.chat.support;

import com.codeit.chat.controller.ChatController;
import com.codeit.chat.service.ChatService;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class HttpLoggingFilterTest {
    private ChatService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ChatService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(service))
                .addFilters(new HttpLoggingFilter()).build();
    }

    @Test
    void logsRequestAndBoundedResponseWithoutChangingActualBody(CapturedOutput output) throws Exception {
        String answer = "가".repeat(1500) + "마지막 문장";
        when(service.answer("안녕!")).thenReturn(answer);

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer test-token-not-real")
                        .content("{\"message\":\"안녕!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(answer));

        assertThat(output).contains("[HTTP REQUEST] POST /api/chat", "[HTTP RESPONSE] status=200", "…")
                .doesNotContain("마지막 문장", "test-token-not-real");
    }

    @Test
    void logsHandledBadRequestAs400(CapturedOutput output) throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(output).contains("[HTTP RESPONSE] status=400");
    }

    @Test
    void doesNotLogSuccessWhenRequestProcessingThrows(CapturedOutput output) {
        when(service.answer("안녕!")).thenThrow(new IllegalStateException("모의 모델 오류"));

        assertThatThrownBy(() -> mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"안녕!\"}")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class);

        assertThat(output).contains("[HTTP FAILED] POST /api/chat")
                .doesNotContain("[HTTP RESPONSE] status=200");
    }
}

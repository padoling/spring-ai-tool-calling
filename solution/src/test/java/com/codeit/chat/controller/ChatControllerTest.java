package com.codeit.chat.controller;

import com.codeit.chat.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatControllerTest {
    private ChatService service;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        service = mock(ChatService.class);
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(service)).build();
    }

    @Test
    void returnsAnswerAsJson() throws Exception {
        when(service.answer("서울 오후 3시를 뉴욕 시간으로 바꿔줘"))
                .thenReturn("변환할 날짜를 알려주세요.");

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"서울 오후 3시를 뉴욕 시간으로 바꿔줘\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.answer").value("변환할 날짜를 알려주세요."));

        verify(service).answer("서울 오후 3시를 뉴욕 시간으로 바꿔줘");
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"message\":null}", "{\"message\":\"\"}", "{\"message\":\"  \"}", "{", "null"})
    void rejectsInvalidBodyBeforeCallingModel(String body) throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void acceptsMessageAtLengthLimit() throws Exception {
        String message = "가".repeat(2000);
        when(service.answer(message)).thenReturn("응답");

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + message + "\"}"))
                .andExpect(status().isOk());

        verify(service).answer(message);
    }

    @Test
    void rejectsOversizedMessageBeforeCallingModel() throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + "가".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}

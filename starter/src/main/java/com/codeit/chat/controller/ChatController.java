package com.codeit.chat.controller;

import com.codeit.chat.controller.dto.ChatRequest;
import com.codeit.chat.controller.dto.ChatResponse;
import com.codeit.chat.service.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message는 비어 있을 수 없습니다.");
        }
        if (request.message().length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "message는 2000자 이하여야 합니다.");
        }
        return new ChatResponse(chatService.answer(request.message()));
    }
}

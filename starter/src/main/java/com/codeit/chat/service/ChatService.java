package com.codeit.chat.service;

import com.codeit.chat.tool.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatService {
    private final ChatClient chatClient;
    private final TimeTools timeTools;
    private final String systemPrompt;

    public ChatService(ChatClient chatClient, TimeTools timeTools,
                       @Value("${chat.system-prompt}") String systemPrompt) {
        this.chatClient = chatClient;
        this.timeTools = timeTools;
        this.systemPrompt = systemPrompt;
    }

    public String answer(String message) {
        // TODO 3: system prompt·질문·도구를 포함해 요청하고 최종 응답 텍스트를 반환하세요.
        throw new UnsupportedOperationException("실습 3: ChatClient 호출을 구현하세요.");
    }
}

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
        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .tools(timeTools)
                .call()
                .content();
    }
}

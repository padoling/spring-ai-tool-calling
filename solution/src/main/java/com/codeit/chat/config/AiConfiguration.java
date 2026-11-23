package com.codeit.chat.config;

import com.codeit.chat.support.TracingToolCallingManager;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    @Bean
    ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }

    @Bean
    @ConditionalOnProperty(prefix = "chat", name = "trace-model", havingValue = "true")
    ToolCallingManager tracingToolCallingManager() {
        return new TracingToolCallingManager(ToolCallingManager.builder().build());
    }
}

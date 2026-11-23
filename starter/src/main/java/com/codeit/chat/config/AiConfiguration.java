package com.codeit.chat.config;

import com.codeit.chat.support.TracingToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfiguration {
    // TODO 1: ChatModel을 주입받아 ChatClient를 Bean으로 등록하세요.

    @Bean
    @ConditionalOnProperty(prefix = "chat", name = "trace-model", havingValue = "true")
    ToolCallingManager tracingToolCallingManager() {
        return new TracingToolCallingManager(ToolCallingManager.builder().build());
    }
}

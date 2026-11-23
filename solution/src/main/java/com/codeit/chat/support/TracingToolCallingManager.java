package com.codeit.chat.support;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.definition.ToolDefinition;

public class TracingToolCallingManager implements ToolCallingManager {
    private static final Logger log = LoggerFactory.getLogger(TracingToolCallingManager.class);
    private final ToolCallingManager delegate;

    public TracingToolCallingManager(ToolCallingManager delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions options) {
        return delegate.resolveToolDefinitions(options);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse response) {
        response.getResults().stream()
                .flatMap(generation -> generation.getOutput().getToolCalls().stream())
                .forEach(call -> log.info("[TOOL CALL] {} args={}", call.name(), LogContent.preview(call.arguments())));
        ToolExecutionResult result = delegate.executeToolCalls(prompt, response);
        if (result.conversationHistory().getLast() instanceof ToolResponseMessage toolMessage) {
            toolMessage.getResponses().forEach(tool ->
                    log.info("[TOOL RESULT] {} result={}", tool.name(), LogContent.preview(tool.responseData())));
        }
        return result;
    }
}

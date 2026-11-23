package com.codeit.chat.service;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.codeit.chat.controller.ChatController;
import com.codeit.chat.config.AiConfiguration;
import com.codeit.chat.support.HttpLoggingFilter;
import com.codeit.chat.tool.TimeTools;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class ToolCallingFlowTest {
    private final ObjectMapper json = new ObjectMapper();
    private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;
    private Client client;
    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        client = Client.builder().apiKey("test-key-not-real")
                .httpOptions(HttpOptions.builder()
                        .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                        .apiVersion("v1beta").timeout(5000).build())
                .build();
    }

    @AfterEach
    void closeServer() {
        if (context != null) {
            context.close();
        }
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsToolResultToGeminiAndAnswersThroughController(CapturedOutput output) throws Exception {
        String date = "2026-07-15";
        String expected = "2026-07-15T02:00-04:00[America/New_York]";
        respondWith(toolCallResponse(date), textResponse("변환 결과 안내"));
        MockMvc mvc = mvc(new TimeTools());

        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"" + date + " 서울 오후 3시는 뉴욕 몇 시야?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("변환 결과 안내"));

        assertThat(requests).hasSize(2);
        List<JsonNode> declarations = requests.getFirst().findValues("functionDeclarations");
        assertThat(declarations).hasSize(1);
        JsonNode function = declarations.getFirst().get(0);
        assertThat(function.path("name").asText()).isEqualTo("convertTime");
        assertThat(function.path("parameters").path("required").toString())
                .contains("dateTime", "sourceZone", "targetZone");
        assertThat(requests.getFirst().findValue("systemInstruction").findValue("text").asText())
                .isEqualTo(context.getEnvironment().getProperty("chat.system-prompt"));
        JsonNode second = requests.get(1);
        JsonNode functionResponse = second.findValue("functionResponse");
        assertThat(functionResponse).isNotNull();
        assertThat(functionResponse.path("name").asText()).isEqualTo("convertTime");
        assertThat(functionResponse.path("response").toString()).contains("sourceTime", "targetTime", expected);
        assertThat(second.findValue("thoughtSignature").asText()).isEqualTo("dGVzdC1zaWduYXR1cmU=");
        String logs = output.toString();
        int httpRequest = logs.indexOf("[HTTP REQUEST]");
        int toolCall = logs.indexOf("[TOOL CALL]");
        int toolResult = logs.indexOf("[TOOL RESULT]");
        int httpResponse = logs.indexOf("[HTTP RESPONSE]");
        assertThat(httpRequest).isGreaterThanOrEqualTo(0);
        assertThat(toolCall).isGreaterThan(httpRequest);
        assertThat(toolResult).isGreaterThan(toolCall);
        assertThat(httpResponse).isGreaterThan(toolResult);
        assertThat(logs).doesNotContain("[TOOL DEFINITIONS]", "[TOOL RESULT MESSAGES]");
    }

    @Test
    void returnsTextWithoutExecutingATool(CapturedOutput output) throws Exception {
        String question = "안녕!";
        String answer = "안녕하세요!";
        respondWith(textResponse(answer));
        TimeTools tools = spy(new TimeTools());

        mvc(tools).perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("message", question))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(answer));

        assertThat(requests).hasSize(1);
        verifyNoInteractions(tools);
        assertThat(output.toString()).contains("[HTTP REQUEST]", "[HTTP RESPONSE]")
                .doesNotContain("[TOOL CALL]", "[TOOL RESULT]");
    }

    @Test
    void requestWithoutToolRegistrationDoesNotExposeOrExecuteTools() throws Exception {
        respondWith(textResponse("도구 없이 생성한 답변"));
        TimeTools tools = spy(new TimeTools());
        mvc(tools);

        assertThat(context.getBean(ChatClient.class).prompt()
                .user("2026년 7월 15일 서울 오후 3시는 뉴욕 몇 시야?")
                .call().content()).isEqualTo("도구 없이 생성한 답변");

        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().findValues("functionDeclarations")).isEmpty();
        verifyNoInteractions(tools);
    }

    private MockMvc mvc(TimeTools tools) throws Exception {
        context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                "test", Map.of("chat.trace-model", "true")));
        for (var source : new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))) {
            context.getEnvironment().getPropertySources().addLast(source);
        }
        context.register(AiConfiguration.class, ChatService.class, ChatController.class);
        context.registerBean(TimeTools.class, () -> tools);
        context.registerBean(ChatModel.class, () -> GoogleGenAiChatModel.builder().genAiClient(client)
                .defaultOptions(GoogleGenAiChatOptions.builder()
                        .model("gemini-test-model").includeThoughts(true).build())
                .toolCallingManager(context.getBean(ToolCallingManager.class))
                .build());
        context.refresh();
        return MockMvcBuilders.standaloneSetup(context.getBean(ChatController.class))
                .addFilters(new HttpLoggingFilter()).build();
    }

    private void respondWith(String... responses) {
        server.createContext("/v1beta/models/gemini-test-model:generateContent", exchange -> {
            int index = requests.size();
            requests.add(json.readTree(exchange.getRequestBody()));
            byte[] body = responses[Math.min(index, responses.length - 1)].getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
            exchange.close();
        });
    }

    private String toolCallResponse(String date) {
        return """
                {"modelVersion":"gemini-test-model","candidates":[{"index":0,"finishReason":"STOP","content":{"role":"model","parts":[{
                "thoughtSignature":"dGVzdC1zaWduYXR1cmU=",
                "functionCall":{"name":"convertTime","args":{"dateTime":"%sT15:00:00",
                "sourceZone":"Asia/Seoul","targetZone":"America/New_York"}}}]}}]}
                """.formatted(date);
    }

    private String textResponse(String content) {
        return """
                {"modelVersion":"gemini-test-model","candidates":[{"index":0,"finishReason":"STOP",
                "content":{"role":"model","parts":[{"text":"%s"}]}}]}
                """.formatted(content);
    }
}

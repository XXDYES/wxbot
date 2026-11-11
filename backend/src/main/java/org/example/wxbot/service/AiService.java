package org.example.wxbot.service;

import org.example.wxbot.config.AiConfig;
import org.example.wxbot.entity.UserAi;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.UserAiManager;
import org.example.wxbot.util.ChatHistoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class AiService {
    @Autowired
    private AiConfig aiConfig;
    @Autowired
    private WebClient aiWebClient;
    @Autowired
    private ChatHistoryUtil chatHistoryUtil;
    @Autowired
    private UserAiManager userAiManager;

    public void aiResponse(Integer userId, String msg, Consumer<String> onMessage) {
        if (msg == null || msg.trim().isEmpty()){
            throw new BusinessException("消息不能为空");
        }

        UserAi userAi = userAiManager.get(userId);
        String text = msg.trim();
        List<Map<String, Object>> input = new ArrayList<>(chatHistoryUtil.load(userId));
        input.add(Map.of(
                "type", "message",
                "role", "user",
                "content", List.of(Map.of("type", "input_text", "text", text))
        ));
        chatHistoryUtil.appendUser(userId, text);

        List<String> replies = new ArrayList<>();
        getAiResponse(input, userAi)
                // 微信发送属于阻塞 IO，放到弹性线程，避免阻塞 SSE 网络线程。
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(reply -> {
                    replies.add(reply);
                    onMessage.accept(reply);
                })
                .blockLast(Duration.ofSeconds(65));

        if (replies.isEmpty()) {
            throw new BusinessException("AI 返回内容为空");
        }
        // 过渡消息只发送给用户，不写入上下文，避免后续模型模仿。
        chatHistoryUtil.appendAssistant(userId, replies.getLast());
    }

    private Flux<String> getAiResponse(List<Map<String, Object>> input, UserAi userAi) {
        return aiWebClient.post()
                .uri("/responses")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(input, userAi))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<JsonNode>>() {
                })
                .handle((event, sink) -> {
                    String reply = extractCompletedMessage(event.data());
                    if (reply != null) {
                        sink.next(reply);
                    }
                });
    }

    private String extractCompletedMessage(JsonNode event) {
        if (event == null) {
            return null;
        }

        String type = event.path("type").asString("");
        if ("error".equals(type) || "response.failed".equals(type)) {
            String message = event.path("error").path("message").asString("");
            throw new BusinessException("AI 返回失败: " + message);
        }
        if ("response.incomplete".equals(type)) {
            String reason = event.path("response")
                    .path("incomplete_details")
                    .path("reason")
                    .asString("");
            throw new BusinessException("AI 返回被截断: " + reason);
        }
        if (!"response.output_item.done".equals(type)) {
            return null;
        }

        JsonNode item = event.path("item");
        if (!"message".equals(item.path("type").asString(""))
                || !"assistant".equals(item.path("role").asString(""))) {
            return null;
        }
        return extractMessageText(item.path("content"));
    }

    private String extractMessageText(JsonNode content) {
        if (!content.isArray()) {
            return null;
        }

        StringBuilder messageText = new StringBuilder();
        for (JsonNode part : content) {
            if ("output_text".equals(part.path("type").asString(""))) {
                String text = part.path("text").asString("");
                if (!text.isBlank()) {
                    if (!messageText.isEmpty()) {
                        messageText.append('\n');
                    }
                    messageText.append(text);
                }
            }
        }
        return messageText.isEmpty() ? null : messageText.toString();
    }

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> input, UserAi userAi) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", valueOrDefault(userAi.getModel(), aiConfig.getModel()));
        requestBody.put("temperature", aiConfig.getTemperature());
        requestBody.put("max_output_tokens", aiConfig.getMaxTokens());
        requestBody.put("stream", true);
        // deepseek官方配置（官方端点只认 deepseek-flash / deepseek-v4-pro）
//        requestBody.put("reasoning", Map.of("effort", "none"));
//        requestBody.put("tools", List.of(Map.of("type", "web_search")));

        // 千问AI平台配置
        requestBody.put("reasoning", Map.of(
                "effort", valueOrDefault(userAi.getReasoningEffort(), "none")
        ));
        requestBody.put("tools", List.of(Map.of("type", "web_search")));
        requestBody.put("max_tool_calls", 2);

        // JSON 输出：text.format 是两层，不能写成 text.type
//        requestBody.put("text", Map.of("format", Map.of("type", "json_object")));

        requestBody.put("instructions", valueOrDefault(userAi.getPersona(), "你是一位聊天机器人"));
        requestBody.put("input", input);
        return requestBody;
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}

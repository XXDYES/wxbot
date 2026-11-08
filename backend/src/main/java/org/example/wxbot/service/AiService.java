package org.example.wxbot.service;

import org.example.wxbot.config.AiConfig;
import org.example.wxbot.entity.UserAi;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.UserAiManager;
import org.example.wxbot.util.ChatHistoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public String aiResponse(Integer userId, String msg){
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

        String reply = getAiResponse(input, userAi)
                .block(Duration.ofSeconds(65));
        if (reply == null || reply.isBlank()) {
            throw new BusinessException("AI 返回内容为空");
        }
        chatHistoryUtil.appendAssistant(userId, reply);
        return reply;
    }

    private Mono<String> getAiResponse(List<Map<String, Object>> input, UserAi userAi){
        return aiWebClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(input, userAi))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(this::extractOutputText);
    }

    private String extractOutputText(JsonNode response) {
        String status = response.path("status").asString("");
        if ("failed".equals(status)) {
            String message = response.path("error").path("message").asString("");
            throw new BusinessException("AI 返回失败: " + message);
        }
        if ("incomplete".equals(status)) {
            String reason = response.path("incomplete_details")
                    .path("reason")
                    .asString("");
            throw new BusinessException("AI 返回被截断: " + reason);
        }

        JsonNode output = response.path("output");
        if (output.isMissingNode() || !output.isArray()) {
            throw new BusinessException("AI 响应中缺少 output 字段");
        }

        for (JsonNode item : output) {
            if (!"message".equals(item.path("type").asString(""))) {
                continue;
            }

            JsonNode content = item.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode part : content) {
                if ("output_text".equals(part.path("type").asString(""))) {
                    String text = part.path("text").asString("");
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        throw new BusinessException("AI 响应中未找到 output_text");
    }

    private Map<String, Object> buildRequestBody(List<Map<String, Object>> input, UserAi userAi) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", valueOrDefault(userAi.getModel(), aiConfig.getModel()));
        requestBody.put("temperature", aiConfig.getTemperature());
        requestBody.put("max_output_tokens", aiConfig.getMaxTokens());
        // 非流式：不设置 stream，默认 false
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

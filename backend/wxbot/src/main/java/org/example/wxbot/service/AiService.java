package org.example.wxbot.service;

import org.example.wxbot.config.AiConfig;
import org.example.wxbot.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    @Autowired
    private AiConfig aiConfig;
    @Autowired
    private WebClient aiWebClient;
    public String aiResponse(String msg){
        if (msg == null || msg.trim().isEmpty()){
            throw new BusinessException("消息不能为空");
        }

        String reply = getAiResponse(msg.trim())
                .block(Duration.ofSeconds(65));
        if (reply == null || reply.isBlank()) {
            throw new BusinessException("AI 返回内容为空");
        }
        return reply;
    }

    private Mono<String> getAiResponse(String msg){
        return aiWebClient.post()
                .uri("/responses")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(msg))
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

    private Map<String, Object> buildRequestBody(String msg) {
        Map<String, Object> requestBody = new HashMap<>();
        // 模型从 application.yml 的 ai.api.model 读取，换模型不用改代码
        requestBody.put("model", aiConfig.getModel());
        requestBody.put("temperature", aiConfig.getTemperature());
        requestBody.put("max_output_tokens", aiConfig.getMaxTokens());
        // 非流式：不设置 stream，默认 false
        // deepseek官方配置（官方端点只认 deepseek-flash / deepseek-v4-pro）
//        requestBody.put("reasoning", Map.of("effort", "none"));
//        requestBody.put("tools", List.of(Map.of("type", "web_search")));

        // 千问AI平台配置
        requestBody.put("enable_thinking", false);
        requestBody.put("tools", List.of(Map.of("type", "web_search")));
        requestBody.put("max_tool_calls", 2);

        // JSON 输出：text.format 是两层，不能写成 text.type
//        requestBody.put("text", Map.of("format", Map.of("type", "json_object")));

        requestBody.put("instructions", "你是一位聊天机器人，你的知识水平相当于一位普通大学生" +
                "并不是无所不知的AI助手");
        requestBody.put("input",msg);
        return requestBody;
    }
}

package org.example.wxbot.service;

import org.apache.tika.Tika;
import org.example.wxbot.config.AiConfig;
import org.example.wxbot.entity.UserAi;
import org.example.wxbot.entity.UserLibrary;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.LibraryManager;
import org.example.wxbot.manager.UserAiManager;
import org.example.wxbot.util.ChatHistoryUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import tools.jackson.databind.JsonNode;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@Slf4j
@Service
public class AiService {
    private static final String TOKENS_TODAY_KEY = "bot:ai:token_today";
    private static final String TOKENS_HISTORY_KEY = "bot:ai:token_history";
    private static final String CHAT_TODAY_KEY = "bot:ai:chat_today";
    private static final String CHAT_HISTORY_KEY = "bot:ai:chat_history";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final Tika TIKA = new Tika();
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    @Autowired
    private AiConfig aiConfig;
    @Autowired
    private WebClient aiWebClient;
    @Autowired
    private ChatHistoryUtil chatHistoryUtil;
    @Autowired
    private UserAiManager userAiManager;
    @Autowired
    private LibraryManager libraryManager;
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void aiResponse(Integer userId, String msg, byte[] image, Consumer<String> onMessage) {
        String text = msg == null ? "" : msg.trim();
        if (text.isEmpty() && image == null) {
            throw new BusinessException("消息不能为空");
        }

        UserAi userAi = userAiManager.get(userId);
        List<Map<String, Object>> input = new ArrayList<>(chatHistoryUtil.load(userId));
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of(
                "type", "input_text",
                "text", text.isEmpty() ? "请理解这张图片并回复用户。" : text
        ));
        if (image != null) {
            content.add(Map.of(
                    "type", "input_image",
                    "image_url", "data:" + detectImageMime(image)
                            + ";base64," + Base64.getEncoder().encodeToString(image)
            ));
        }
        input.add(Map.of(
                "type", "message",
                "role", "user",
                "content", content
        ));
        chatHistoryUtil.appendUser(userId, historyText(text, image));

        List<String> replies = new ArrayList<>();
        getAiResponse(input, userAi, userId)
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

    private Flux<String> getAiResponse(
            List<Map<String, Object>> input,
            UserAi userAi,
            Integer userId
    ) {
        return aiWebClient.post()
                .uri("/responses")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(buildRequestBody(input, userAi, userId))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<JsonNode>>() {
                })
                .handle((event, sink) -> {
                    JsonNode data = event.data();
                    if (data == null) {
                        return;
                    }
                    if ("response.completed".equals(data.path("type").asString(""))) {
                        updateUsage(data.path("response")
                                .path("usage")
                                .path("total_tokens")
                                .asLong(0),userAi.getUserId());
                    }
                    String reply = extractCompletedMessage(data);
                    if (reply != null) {
                        sink.next(reply);
                    }
                });
    }

    public Map<String, Long> getUsage(Integer userId) {
        String id = String.valueOf(userId);
        return Map.of(
                "chatTimesToday", getCounter(CHAT_TODAY_KEY + ":" + id),
                "chatTimesHistory", getCounter(CHAT_HISTORY_KEY + ":" + id),
                "tokenToday", getCounter(TOKENS_TODAY_KEY + ":" + id),
                "tokenHistory", getCounter(TOKENS_HISTORY_KEY + ":" + id)
        );
    }

    public Map<String, String> getAiInfo(Integer userId) {
        UserAi userAi = userAiManager.get(userId);
        return Map.of(
                "model", valueOrDefault(userAi.getModel(), aiConfig.getModel()),
                "reasoningEffort", valueOrDefault(userAi.getReasoningEffort(), "none")
        );
    }

    private long getCounter(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0 : Long.parseLong(value);
        } catch (Exception exception) {
            log.warn("读取 AI 使用统计失败 | key={}", key, exception);
            return 0;
        }
    }

    private void updateUsage(long tokens, Integer userId) {
        if (userId == null || tokens < 0) {
            return;
        }
        try {
            String id = String.valueOf(userId);
            String tokensTodayKey = TOKENS_TODAY_KEY + ":" + id;
            String chatTodayKey = CHAT_TODAY_KEY + ":" + id;
            if (tokens > 0) {
                redisTemplate.opsForValue().increment(tokensTodayKey, tokens);
                redisTemplate.opsForValue().increment(TOKENS_HISTORY_KEY + ":" + id, tokens);
            }
            Long chats = redisTemplate.opsForValue().increment(chatTodayKey, 1);
            redisTemplate.opsForValue().increment(CHAT_HISTORY_KEY + ":" + id, 1);
            if (Long.valueOf(1).equals(chats)) {
                Instant nextMidnight = LocalDate.now(ZONE)
                        .plusDays(1)
                        .atStartOfDay(ZONE)
                        .toInstant();
                redisTemplate.expireAt(tokensTodayKey, nextMidnight);
                redisTemplate.expireAt(chatTodayKey, nextMidnight);
            }
        } catch (Exception exception) {
            log.warn("累计总 Token 消耗失败", exception);
        }
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

    private Map<String, Object> buildRequestBody(
            List<Map<String, Object>> input,
            UserAi userAi,
            Integer userId
    ) {
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

        requestBody.put("instructions", buildInstructions(userAi, userId));
        requestBody.put("input", input);
        return requestBody;
    }

    private String buildInstructions(UserAi userAi, Integer userId) {
        StringBuilder instructions = new StringBuilder(
                valueOrDefault(userAi.getPersona(), "你是一位聊天机器人")
        );
        List<UserLibrary> libraries = libraryManager.get(userId);
        if (libraries.isEmpty()) {
            return instructions.toString();
        }

        instructions.append("\n\n【知识库】");
        for (UserLibrary library : libraries) {
            instructions.append("\n\n### ")
                    .append(valueOrDefault(library.getTitle(), "未命名知识库"));
            if (library.getDescription() != null && !library.getDescription().isBlank()) {
                instructions.append("\n描述：").append(library.getDescription());
            }
            instructions.append("\n内容：")
                    .append(valueOrDefault(library.getContent(), ""));
        }
        return instructions.toString();
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String historyText(String text, byte[] image) {
        if (image == null) {
            return text;
        }
        return (text.isEmpty() ? "" : text + "\n") + "[用户发送了一张图片]";
    }

    private String detectImageMime(byte[] image) {
        String mimeType = TIKA.detect(image);
        if (!SUPPORTED_IMAGE_TYPES.contains(mimeType)) {
            throw new BusinessException("不支持的图片格式: " + mimeType);
        }
        return mimeType;
    }
}

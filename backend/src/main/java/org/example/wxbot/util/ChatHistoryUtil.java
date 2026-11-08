package org.example.wxbot.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChatHistoryUtil {

    private static final String KEY_PREFIX = "bot:chat:history:";
    private static final int MAX_ITEMS = 20;
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatHistoryUtil(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void appendUser(Integer userId, String text) {
        append(userId, "user", text);
    }

    public void appendAssistant(Integer userId, String text) {
        append(userId, "assistant", text);
    }

    /**
     * 返回可直接放入 Responses API input 的历史消息。
     */
    public List<Map<String, Object>> load(Integer userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        try {
            List<String> raw = redisTemplate.opsForList().range(key(userId), 0, -1);
            if (raw == null || raw.isEmpty()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> history = new ArrayList<>(raw.size());
            for (String json : raw) {
                try {
                    history.add(objectMapper.readValue(
                            json,
                            new TypeReference<Map<String, Object>>() {
                            }
                    ));
                } catch (Exception exception) {
                    log.warn("对话历史反序列化失败，已跳过该条 | userId={}", userId, exception);
                }
            }
            return history;
        } catch (Exception exception) {
            log.warn("读取对话历史失败 | userId={}", userId, exception);
            return new ArrayList<>();
        }
    }

    public void clear(Integer userId) {
        if (userId == null) {
            return;
        }
        try {
            redisTemplate.delete(key(userId));
        } catch (Exception exception) {
            log.warn("清空对话历史失败 | userId={}", userId, exception);
        }
    }

    private void append(Integer userId, String role, String text) {
        if (userId == null || isBlank(text)) {
            log.warn("对话历史参数不完整，跳过写入 | userId={} | role={}", userId, role);
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(buildItem(role, text));
            String key = key(userId);
            redisTemplate.opsForList().rightPush(key, json);
            redisTemplate.opsForList().trim(key, -MAX_ITEMS, -1);
            redisTemplate.expire(key, TTL);
        } catch (Exception exception) {
            // Redis 异常不能影响本次 AI 回复。
            log.warn("写入对话历史失败 | userId={} | role={}", userId, role, exception);
        }
    }

    private Map<String, Object> buildItem(String role, String text) {
        String contentType = "assistant".equals(role) ? "output_text" : "input_text";
        return Map.of(
                "type", "message",
                "role", role,
                "content", List.of(Map.of(
                        "type", contentType,
                        "text", text
                ))
        );
    }

    private String key(Integer userId) {
        return KEY_PREFIX + userId;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

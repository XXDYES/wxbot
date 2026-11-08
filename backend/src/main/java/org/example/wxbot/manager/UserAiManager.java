package org.example.wxbot.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.config.AiConfig;
import org.example.wxbot.entity.UserAi;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.mapper.UserAiMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class UserAiManager implements ApplicationRunner {

    private final Map<Integer, UserAi> cache = new ConcurrentHashMap<>();
    private final UserAiMapper userAiMapper;
    private final AiConfig aiConfig;

    public UserAiManager(UserAiMapper userAiMapper, AiConfig aiConfig) {
        this.userAiMapper = userAiMapper;
        this.aiConfig = aiConfig;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            userAiMapper.selectList(null)
                    .forEach(config -> cache.put(config.getUserId(), config));
            log.info("用户 AI 配置缓存加载完成，共 {} 条", cache.size());
        } catch (Exception exception) {
            // 数据库暂时不可用时仍允许应用启动，后续访问会按 userId 回源。
            log.error("用户 AI 配置缓存加载失败，将在首次访问时回源", exception);
        }
    }

    public UserAi get(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        return cache.computeIfAbsent(userId, this::loadOrCreate);
    }

    public UserAi createDefault(Integer userId) {
        UserAi config = insertDefault(userId);
        cache.put(userId, config);
        return config;
    }

    public void put(UserAi config) {
        if (config == null || config.getUserId() == null) {
            throw new BusinessException("用户 AI 配置不能为空");
        }
        cache.put(config.getUserId(), config);
    }

    public void remove(Integer userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private UserAi loadOrCreate(Integer userId) {
        UserAi config = userAiMapper.getAiSetting(userId);
        return config != null ? config : insertDefault(userId);
    }

    private UserAi insertDefault(Integer userId) {
        UserAi config = defaultConfig(userId);
        if (userAiMapper.insert(config) <= 0) {
            throw new BusinessException("用户 AI 配置创建失败");
        }
        return config;
    }

    private UserAi defaultConfig(Integer userId) {
        UserAi config = new UserAi();
        config.setUserId(userId);
        config.setModel(aiConfig.getModel());
        config.setReasoningEffort("none");
        config.setPersona("你是一位聊天机器人");
        return config;
    }
}

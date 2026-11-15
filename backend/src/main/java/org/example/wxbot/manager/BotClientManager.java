package org.example.wxbot.manager;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import com.github.wechat.ilink.sdk.core.context.ResumeContext;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.entity.UserBot;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.mapper.UserBotMapper;
import org.example.wxbot.service.PollingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
public class BotClientManager implements ApplicationRunner {

    private static final String CURSOR_KEY_PREFIX = "bot:updates:cursor:";

    private final Map<Integer, ILinkClient> botClients = new ConcurrentHashMap<>();
    private volatile boolean shuttingDown;

    @Autowired
    private UserBotMapper userBotMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    @Qualifier("botExecutor")
    private ExecutorService botExecutor;
    @Autowired
    @Lazy
    private PollingService pollingService;

    @Override
    public void run(ApplicationArguments args) {
        botExecutor.submit(this::restoreAll);
    }

    @PreDestroy
    public void shutdown() {
        shuttingDown = true;
        botClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception exception) {
                log.warn("关闭机器人客户端失败", exception);
            }
        });
        botClients.clear();
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public ILinkClient get(Integer userId) {
        return botClients.get(userId);
    }

    public ILinkClient getOrCreate(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        return botClients.computeIfAbsent(userId, ignored -> createClient());
    }

    public boolean contains(Integer userId) {
        return botClients.containsKey(userId);
    }

    public ILinkClient putIfAbsent(Integer userId, ILinkClient client) {
        return botClients.putIfAbsent(userId, client);
    }

    public void remove(Integer userId, ILinkClient client) {
        if (userId != null && client != null) {
            botClients.remove(userId, client);
        }
    }

    public void saveLoginContext(Integer userId, LoginContext loginContext) {
        UserBot userBot = new UserBot();
        userBot.setUserId(userId);
        userBot.setIlinkUserId(loginContext.getUserId());
        userBot.setBotId(loginContext.getBotId());
        userBot.setBotToken(loginContext.getBotToken());
        userBot.setBaseUrl(loginContext.getBaseUrl());

        UserBot existing = userBotMapper.findLatestByUserId(userId);
        if (existing == null) {
            userBotMapper.insert(userBot);
        } else {
            userBot.setId(existing.getId());
            userBotMapper.updateById(userBot);
        }
    }

    public void saveUpdatesCursor(Integer userId, ILinkClient client) {
        if (shuttingDown) {
            return;
        }
        try {
            ResumeContext snapshot = client.exportResumeContext();
            if (snapshot == null || snapshot.getUpdatesCursor() == null) {
                return;
            }
            redisTemplate.opsForValue()
                    .set(cursorKey(userId), snapshot.getUpdatesCursor());
        } catch (Exception exception) {
            log.warn("保存 updatesCursor 失败 | userId={}", userId, exception);
        }
    }

    public void invalidateSession(Integer userId, ILinkClient client) {
        if (userId == null || client == null) {
            return;
        }
        if (!botClients.remove(userId, client)) {
            return;
        }

        try {
            redisTemplate.delete(cursorKey(userId));
        } catch (Exception exception) {
            log.warn("清理 Redis cursor 失败 | userId={}", userId, exception);
        }
        try {
            userBotMapper.deleteByUserId(userId);
        } catch (Exception exception) {
            log.warn("清理机器人登录信息失败 | userId={}", userId, exception);
        }
        try {
            client.close();
        } catch (Exception exception) {
            log.warn("关闭失效客户端失败 | userId={}", userId, exception);
        }
    }

    public void unbind(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }

        try {
            userBotMapper.deleteByUserId(userId);
        } catch (Exception exception) {
            log.error("删除机器人绑定信息失败 | userId={}", userId, exception);
            throw new BusinessException("解除机器人绑定失败");
        }

        ILinkClient client = botClients.remove(userId);
        if (client != null) {
            pollingService.stopPolling(userId);
            try {
                client.close();
            } catch (Exception exception) {
                log.warn("关闭机器人客户端失败 | userId={}", userId, exception);
            }
        }

        try {
            redisTemplate.delete(cursorKey(userId));
        } catch (Exception exception) {
            log.warn("清理 Redis cursor 失败 | userId={}", userId, exception);
        }
    }

    private void restoreAll() {
        try {
            List<UserBot> userBots = userBotMapper.selectList(null);
            int restored = 0;
            for (UserBot userBot : userBots) {
                if (restore(userBot)) {
                    restored++;
                }
            }
            log.info("机器人登录状态恢复完成，成功 {} 个", restored);
        } catch (Exception exception) {
            log.error("机器人登录状态恢复失败", exception);
        }
    }

    private boolean restore(UserBot userBot) {
        Integer userId = userBot.getUserId();
        if (userId == null || botClients.containsKey(userId)) {
            return false;
        }

        try {
            LoginContext loginContext = new LoginContext(
                    userBot.getBotToken(),
                    userBot.getIlinkUserId(),
                    userBot.getBotId(),
                    userBot.getBaseUrl()
            );

            ResumeContext.Builder builder = ResumeContext.builder(loginContext);
            String cursor = loadUpdatesCursor(userId);
            if (cursor != null && !cursor.isBlank()) {
                builder.updatesCursor(cursor);
            }

            ILinkClient client = ILinkClient.builder()
                    .config(ILinkConfig.builder().heartbeatEnabled(false).build())
                    .resumeContext(builder.build())
                    .build();

            if (putIfAbsent(userId, client) != null) {
                client.close();
                return false;
            }
            pollingService.startPolling(client, userId);
            log.info("机器人登录状态已恢复 | userId={} | botId={}", userId, userBot.getBotId());
            return true;
        } catch (Exception exception) {
            log.error("恢复机器人失败 | userId={}", userId, exception);
            return false;
        }
    }

    private String loadUpdatesCursor(Integer userId) {
        try {
            return redisTemplate.opsForValue().get(cursorKey(userId));
        } catch (Exception exception) {
            log.warn("读取 updatesCursor 失败，将从当前位置开始 | userId={}", userId, exception);
            return null;
        }
    }

    private String cursorKey(Integer userId) {
        return CURSOR_KEY_PREFIX + userId;
    }

    private ILinkClient createClient() {
        return ILinkClient.builder()
                .config(ILinkConfig.builder().heartbeatEnabled(false).build())
                .build();
    }
}

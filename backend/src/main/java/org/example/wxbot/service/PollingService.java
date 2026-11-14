package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.exception.NotLoginException;
import com.github.wechat.ilink.sdk.core.exception.SessionExpiredException;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.BotClientManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class PollingService {

    private static final int MAX_IMAGE_BYTES = 2 * 1024 * 1024;

    @Autowired
    @Qualifier("botExecutor")
    private ExecutorService botExecutor;
    @Autowired
    private AiService aiService;
    @Autowired
    private BotClientManager botClientManager;
    @Autowired
    private BotCommandService botCommandService;

    private final Map<Integer, Thread> pollingThreads = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdown() {
        pollingThreads.values().forEach(Thread::interrupt);
        pollingThreads.clear();
    }

    public void startPolling(ILinkClient client, Integer userId) {
        botExecutor.submit(() -> {
            Thread pollingThread = Thread.currentThread();
            pollingThreads.put(userId, pollingThread);
            try {
                poll(client, userId);
            } finally {
                pollingThreads.remove(userId, pollingThread);
            }
        });
    }

    private void poll(ILinkClient client, Integer userId) {
        while (!Thread.currentThread().isInterrupted()
                && !botClientManager.isShuttingDown()) {
                List<WeixinMessage> messages;
                try {
                    messages = client.getUpdates();
                    if (botClientManager.isShuttingDown()) {
                        break;
                    }
                    botClientManager.saveUpdatesCursor(userId, client);
                }catch (SessionExpiredException exception){
                    log.warn("机器人会话已失效，停止轮询 | userId={}", userId);
                    botClientManager.invalidateSession(userId, client);
                    break;
                } catch (NotLoginException exception) {
                    log.error("机器人已掉线", exception);
                    botClientManager.invalidateSession(userId, client);
                    break;
                } catch (Exception exception) {
                    if (botClientManager.isShuttingDown()
                            || Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    log.warn("消息拉取失败，3 秒后重试", exception);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    continue;
                }

                for (WeixinMessage message : messages) {
                    try {
                        String fromUserId = message.getFrom_user_id();
                        if (fromUserId == null
                                || !Integer.valueOf(1).equals(message.getMessage_type())) continue;
                        MessageContent content = extractContent(message);
                        if (botCommandService.handle(
                                client, userId, fromUserId, content.text())) {
                            continue;
                        }
                        if (content.unsupported()
                                || (content.text() == null && content.image() == null)) {
                            client.sendText(fromUserId, "抱歉，暂仅支持语音、文字和图片消息");
                            continue;
                        }
                        Long sentAt = message.getCreate_time_ms();
                        Long delayMs = sentAt == null
                                ? null
                                : System.currentTimeMillis() - sentAt;

                        log.info("收到 [{}] 的消息: {}, 延迟={}ms",
                                fromUserId,
                                content.text() == null ? "[图片]" : content.text(),
                                delayMs == null ? "未知" : delayMs);
                        botExecutor.submit(() -> {
                            try {
                                replyWithTyping(
                                        client,
                                        userId,
                                        fromUserId,
                                        content.text(),
                                        content.image()
                                );
                            } catch (Exception exception) {
                                log.error("AI 回复失败", exception);
                            }
                        });
                    } catch (Exception exception) {
                        log.error("回复消息失败, messageId={}",
                                message.getMessage_id(), exception);
                    }
                }
        }
    }

    private void replyWithTyping(
            ILinkClient client,
            Integer userId,
            String fromUserId,
            String text,
            MessageItem imageItem
    )
            throws IOException {
        client.startTyping(fromUserId);
        try {
            byte[] image = imageItem == null
                    ? null
                    : client.downloadImageFromMessageItem(imageItem);
            if (image != null && image.length > MAX_IMAGE_BYTES) {
                image = client.downloadImageThumbFromMessageItem(imageItem);
            }
            aiService.aiResponse(userId, text, image, message -> {
                try {
                    client.sendText(fromUserId, message);
                } catch (IOException exception) {
                    throw new RuntimeException("发送 AI 回复失败", exception);
                }
            });
        } catch (BusinessException exception) {
            String errorMessage = exception.getMsg();
            client.sendText(
                    fromUserId,
                    errorMessage == null || errorMessage.isBlank()
                            ? "消息处理失败，请稍后重试"
                            : errorMessage
            );
        } finally {
            try {
                client.stopTyping(fromUserId);
            } catch (Exception exception) {
                log.warn("停止正在输入状态失败, userId={}", fromUserId, exception);
            }
        }
    }

    private MessageContent extractContent(WeixinMessage message) {
        if (message.getItem_list() == null || message.getItem_list().isEmpty()) {
            return new MessageContent(null, null, false);
        }

        StringBuilder text = new StringBuilder();
        MessageItem image = null;
        boolean unsupported = false;
        for (MessageItem item : message.getItem_list()) {
            if (item.getText_item() != null && item.getText_item().getText() != null) {
                text.append(item.getText_item().getText());
            } else if (item.getVoice_item() != null && item.getVoice_item().getText() != null) {
                text.append(item.getVoice_item().getText());
            } else if (item.getImage_item() != null && image == null) {
                image = item;
            } else if (item.getVoice_item() == null && item.getImage_item() == null) {
                unsupported = true;
            }
        }
        String result = text.toString().trim();
        return new MessageContent(result.isEmpty() ? null : result, image, unsupported);
    }

    private record MessageContent(String text, MessageItem image, boolean unsupported) {
    }
}

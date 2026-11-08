package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.exception.NotLoginException;
import com.github.wechat.ilink.sdk.core.exception.SessionExpiredException;
import com.github.wechat.ilink.sdk.core.model.MessageItem;
import com.github.wechat.ilink.sdk.core.model.WeixinMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class PollingService {

    @Autowired
    @Qualifier("botExecutor")
    private ExecutorService botExecutor;
    @Autowired
    private AiService aiService;

    public void startPolling(ILinkClient client, Integer userId) {
        botExecutor.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                List<WeixinMessage> messages;
                try {
                    messages = client.getUpdates();
                }catch (SessionExpiredException exception){
                    log.warn("机器人会话已失效，停止轮询 | userId={}", userId);
                    break;
                } catch (NotLoginException exception) {
                    log.error("机器人已掉线", exception);
                    break;
                } catch (Exception exception) {
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
                        String text = extractText(message);
                        if (fromUserId == null || text == null) {
                            continue;
                        }
                        Long sentAt = message.getCreate_time_ms();
                        Long delayMs = sentAt == null
                                ? null
                                : System.currentTimeMillis() - sentAt;

                        log.info("收到 [{}] 的消息: {}, messageId={}, 延迟={}ms",
                                fromUserId, text, message.getMessage_id(),
                                delayMs == null ? "未知" : delayMs);
                        botExecutor.submit(() -> {
                            try {
                                replyWithTyping(client, userId, fromUserId, text);
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
        });
    }

    private void replyWithTyping(ILinkClient client, Integer userId, String fromUserId, String text)
            throws IOException {
        client.startTyping(fromUserId);
        try {
            String answer = aiService.aiResponse(userId, text);
            client.sendText(fromUserId, answer);
        } finally {
            try {
                client.stopTyping(fromUserId);
            } catch (Exception exception) {
                log.warn("停止正在输入状态失败, userId={}", fromUserId, exception);
            }
        }
    }

    private String extractText(WeixinMessage message) {
        // 只处理用户发送的消息，机器人自己发送的消息 type 为 2。
        if (!Integer.valueOf(1).equals(message.getMessage_type())) {
            return null;
        }
        if (message.getItem_list() == null || message.getItem_list().isEmpty()) {
            return null;
        }

        StringBuilder text = new StringBuilder();
        for (MessageItem item : message.getItem_list()) {
            if (item.getText_item() != null && item.getText_item().getText() != null) {
                text.append(item.getText_item().getText());
            } else if (item.getVoice_item() != null && item.getVoice_item().getText() != null) {
                text.append(item.getVoice_item().getText());
            }
        }
        String result = text.toString().trim();
        return result.isEmpty() ? null : result;
    }
}

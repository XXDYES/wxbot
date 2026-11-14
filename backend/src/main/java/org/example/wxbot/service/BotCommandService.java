package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import org.example.wxbot.util.ChatHistoryUtil;
import org.example.wxbot.util.ImageUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Service
public class BotCommandService {

    private final ChatHistoryUtil chatHistoryUtil;
    private final AiService aiService;
    private final byte[] flyImage = ImageUtil.fromClasspath("/images/eagle.png");
    private final byte[] genshinImage = ImageUtil.fromClasspath("/images/genshin.jpg");

    public BotCommandService(ChatHistoryUtil chatHistoryUtil, AiService aiService) {
        this.chatHistoryUtil = chatHistoryUtil;
        this.aiService = aiService;
    }

    public boolean handle(
            ILinkClient client,
            Integer userId,
            String fromUserId,
            String text
    ) throws IOException {
        if (text == null) {
            return false;
        }

        String commandText = text.trim();
        if (!commandText.startsWith("/")) {
            return false;
        }

        String command = commandText.substring(1)
                .trim()
                .split("\\s+", 2)[0]
                .toLowerCase(Locale.ROOT);

        switch (command) {
            case "reload" -> {
                chatHistoryUtil.clear(userId);
                client.sendText(fromUserId, "命令已执行：上下文已经清空");
            }
            case "fly" -> {
                client.sendImage(
                        fromUserId,
                        flyImage,
                        "eagle.png",
                        ""
                );
            }
            case "genshin", "原神" -> {
                client.sendImage(
                        fromUserId,
                        genshinImage,
                        "genshin.jpg",
                        ""
                );
            }
            case "usage", "token" -> {
                Map<String, Long> usage = aiService.getUsage(userId);
                client.sendText(
                        fromUserId,
                        "今日消耗 Token：%s\n历史消耗 Token：%s".formatted(
                                formatTokens(usage.get("tokenToday")),
                                formatTokens(usage.get("tokenHistory"))
                        )
                );
            }
            case "ai" -> {
                Map<String, String> aiInfo = aiService.getAiInfo(userId);
                client.sendText(
                        fromUserId,
                        "当前模型：%s\n思考强度：%s".formatted(
                                aiInfo.get("model"),
                                aiInfo.get("reasoningEffort")
                        )
                );
            }
            default -> client.sendText(fromUserId, "未知命令：" + command);
        }
        return true;
    }

    private String formatTokens(long value) {
        if (value >= 1_000_000_000) {
            return String.format(Locale.ROOT, "%.2fB", value / 1_000_000_000.0);
        }
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.2fM", value / 1_000_000.0);
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.2fK", value / 1_000.0);
        }
        return String.valueOf(value);
    }

}

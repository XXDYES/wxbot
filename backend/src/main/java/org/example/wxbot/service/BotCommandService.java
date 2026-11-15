package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.util.ChatHistoryUtil;
import org.example.wxbot.util.ImageUtil;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
public class BotCommandService {

    private static final String HELP_TEXT = """
            当前可用命令（不区分大小写）：
            /help - 查看命令列表
            /reload - 清空当前对话上下文
            /usage 或 /token - 查看今日和历史 Token 消耗
            /AI - 查看当前模型和思考强度
            /fly - 开启飞行模式
            /genshin 或 /原神 - 懂你意思
            """.strip();

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

        log.info("执行命令 | userId={} | command:/{}", userId, command);

        switch (command) {
            case "help" -> client.sendText(fromUserId, HELP_TEXT);
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

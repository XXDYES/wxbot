package org.example.wxbot.controller;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.state.ConnectionStatus;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.common.Result;
import org.example.wxbot.common.UserContext;
import org.example.wxbot.manager.BotClientManager;
import org.example.wxbot.service.BotService;
import org.example.wxbot.util.ChatHistoryUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class BotController {
    @Autowired
    private BotClientManager botClientManager;
    @Autowired
    private BotService botService;
    @Autowired
    private ChatHistoryUtil chatHistoryUtil;

    @GetMapping("/createBot")
    public Result<String> createBot(){
        Integer userId = UserContext.getCurrentId();
        ILinkClient client = botClientManager.getOrCreate(userId);
        String qrcode = botService.botLogin(client, userId);
        return Result.success(qrcode);
    }

    @GetMapping("/connectTest")
    public Result<String> connectTest() {
        Integer userId = UserContext.getCurrentId();
        ILinkClient client = botClientManager.get(userId);
        if (client == null) {
            log.warn("连接测试失败 | userId={} | 机器人未登录", userId);
            return Result.error("机器人未登录");
        }

        ConnectionStatus status = client.getConnectionStatus();
        String connectionStatus = status == ConnectionStatus.LOGGED_IN
                || (status == ConnectionStatus.DISCONNECTED && client.getLoginContext() != null)
                ? "CONNECTED" : status.name();
        log.info("连接测试完成 | userId={} | status={}", userId, connectionStatus);
        return Result.success(connectionStatus);
    }

    @PostMapping("/clearContext")
    public Result<Void> clearContext() {
        Integer userId = UserContext.getCurrentId();
        chatHistoryUtil.clear(userId);
        log.info("清空上下文完成 | userId={}", userId);
        return Result.success();
    }

    @PostMapping("/unbindBot")
    public Result<Void> unbindBot() {
        Integer userId = UserContext.getCurrentId();
        botClientManager.unbind(userId);
        log.info("解除机器人绑定完成 | userId={}", userId);
        return Result.success();
    }
}

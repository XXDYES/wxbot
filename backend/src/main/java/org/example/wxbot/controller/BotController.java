package org.example.wxbot.controller;

import com.github.wechat.ilink.sdk.ILinkClient;
import org.example.wxbot.common.Result;
import org.example.wxbot.common.UserContext;
import org.example.wxbot.manager.BotClientManager;
import org.example.wxbot.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BotController {
    @Autowired
    private BotClientManager botClientManager;
    @Autowired
    private BotService botService;

    @GetMapping("/createBot")
    public Result<String> createBot(){
        Integer userId = UserContext.getCurrentId();
        ILinkClient client = botClientManager.getOrCreate(userId);
        String qrcode = botService.botLogin(client, userId);
        return Result.success(qrcode);
    }
}

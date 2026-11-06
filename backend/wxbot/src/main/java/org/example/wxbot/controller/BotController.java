package org.example.wxbot.controller;

import com.github.wechat.ilink.sdk.ILinkClient;
import org.example.wxbot.common.Result;
import org.example.wxbot.manager.BotClientManager;
import org.example.wxbot.service.BotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.ExecutorService;

@RestController
public class BotController {
    @Autowired
    private BotClientManager botClientManager;
    @Autowired
    @Qualifier("botExecutor")
    private ExecutorService botExecutor;
    @Autowired
    private BotService botService;

    @GetMapping("/createBot")
    public Result<String> createBot(){
        ILinkClient client = botClientManager.createClient();
        String qrcode = botService.botLogin(client);
        return Result.success(qrcode);
    }
}

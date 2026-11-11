package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.manager.BotClientManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.github.wechat.ilink.sdk.core.login.LoginContext;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

@Service
@Slf4j
public class BotService {
    @Autowired
    @Qualifier("botExecutor")
    private ExecutorService botExecutor;
    @Autowired
    private PollingService pollingService;
    @Autowired
    private BotClientManager botClientManager;

    public String botLogin(ILinkClient client, Integer userId){
        String qrcode = client.executeLogin();
        botExecutor.submit(()->{
            try {
                LoginContext ctx = client.getLoginFuture().get();
                log.info("扫码成功，botId = {}", ctx.getBotId());
                try {
                    botClientManager.saveLoginContext(userId, ctx);
                } catch (Exception exception) {
                    log.error("机器人登录状态持久化失败 | userId={}", userId, exception);
                }
                pollingService.startPolling(client, userId);
            } catch (InterruptedException e) {
                botClientManager.remove(userId, client);
                Thread.currentThread().interrupt();
            }catch (ExecutionException e){
                botClientManager.remove(userId, client);
                log.error("扫码登录失败 | userId={}", userId, e);
            }
        });
        return qrcode;
    }
}

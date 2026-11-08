package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import lombok.extern.slf4j.Slf4j;
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

    public String botLogin(ILinkClient client, Integer userId){
        String qrcode = client.executeLogin();
        botExecutor.submit(()->{
            try {
                LoginContext ctx = client.getLoginFuture().get();
                log.info("扫码成功，botId = {}", ctx.getBotId());
                pollingService.startPolling(client, userId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }catch (ExecutionException e){
                throw new RuntimeException(e);
            }
        });
        return qrcode;
    }
}

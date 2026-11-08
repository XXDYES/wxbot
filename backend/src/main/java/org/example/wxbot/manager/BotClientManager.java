package org.example.wxbot.manager;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.config.ILinkConfig;
import org.springframework.stereotype.Component;

@Component
public class BotClientManager {
    public ILinkClient createClient() {
        return ILinkClient.builder()
                .config(ILinkConfig.builder().heartbeatEnabled(false).build())
                .build();
    }
}

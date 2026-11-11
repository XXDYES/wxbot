package org.example.wxbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Configuration
public class BotConfig {
    @Bean(name = "botExecutor", destroyMethod = "shutdownNow")
    public ExecutorService botExecutor(){
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    @Bean
    public Semaphore aiSemaphore(){
        return new Semaphore(10);
    }
}

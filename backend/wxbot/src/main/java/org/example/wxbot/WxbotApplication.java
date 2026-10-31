package org.example.wxbot;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("org.example.wxbot.mapper")
public class WxbotApplication {

    public static void main(String[] args) {
        SpringApplication.run(WxbotApplication.class, args);
    }

}

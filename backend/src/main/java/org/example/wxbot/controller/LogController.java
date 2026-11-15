package org.example.wxbot.controller;

import org.example.wxbot.common.log.LogStreamManager;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class LogController {

    private final LogStreamManager logStreamManager;

    public LogController(LogStreamManager logStreamManager) {
        this.logStreamManager = logStreamManager;
    }

    @GetMapping(
            value = "/logs/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter stream(@RequestParam Integer userId) {
        return logStreamManager.subscribe(userId);
    }
}

package org.example.wxbot.common.log;

public record LogEvent(
        long id,
        String time,
        String level,
        String message,
        Integer userId,
        String throwable
) {
}

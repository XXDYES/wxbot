package org.example.wxbot.common.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

public class LogStreamAppender extends AppenderBase<ILoggingEvent> {

    private static final String BASE_PACKAGE = "org.example.wxbot";
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicLong idGenerator = new AtomicLong();
    private final LogStreamManager logStreamManager;

    public LogStreamAppender(LogStreamManager logStreamManager) {
        this.logStreamManager = logStreamManager;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!event.getLoggerName().startsWith(BASE_PACKAGE)
                || !event.getLevel().isGreaterOrEqual(Level.INFO)) {
            return;
        }

        Integer userId = readUserId(event);
        if (userId == null) {
            return;
        }

        logStreamManager.publish(new LogEvent(
                idGenerator.incrementAndGet(),
                Instant.ofEpochMilli(event.getTimeStamp())
                        .atZone(ZONE)
                        .format(TIME_FORMAT),
                event.getLevel().toString(),
                event.getFormattedMessage(),
                userId,
                throwableSummary(event.getThrowableProxy())
        ));
    }

    private Integer readUserId(ILoggingEvent event) {
        String userId = event.getMDCPropertyMap().get("userId");
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            return Integer.valueOf(userId);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String throwableSummary(IThrowableProxy throwable) {
        if (throwable == null) {
            return null;
        }

        String message = throwable.getMessage();
        return message == null || message.isBlank()
                ? throwable.getClassName()
                : throwable.getClassName() + ": " + message;
    }
}

package org.example.wxbot.common.log;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PreDestroy;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class LogStreamAppenderRegistrar {

    private static final String APPENDER_NAME = "LOG_SSE_STREAM";

    private final LogStreamManager logStreamManager;
    private LogStreamAppender appender;

    public LogStreamAppenderRegistrar(LogStreamManager logStreamManager) {
        this.logStreamManager = logStreamManager;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void register() {
        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        if (root.getAppender(APPENDER_NAME) != null) {
            return;
        }

        appender = new LogStreamAppender(logStreamManager);
        appender.setName(APPENDER_NAME);
        appender.setContext(context);
        appender.start();
        root.addAppender(appender);
    }

    @PreDestroy
    public void unregister() {
        if (appender == null) {
            return;
        }

        LoggerContext context =
                (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        root.detachAppender(appender);
        appender.stop();
        appender = null;
    }
}

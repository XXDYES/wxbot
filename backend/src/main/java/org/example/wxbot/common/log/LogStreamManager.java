package org.example.wxbot.common.log;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class LogStreamManager {

    private static final int MAX_QUEUE_SIZE = 500;
    private static final long HEARTBEAT_SECONDS = 20;

    private final Map<Integer, SseEmitter> subscribers = new ConcurrentHashMap<>();
    private final BlockingQueue<LogEvent> logQueue =
            new ArrayBlockingQueue<>(MAX_QUEUE_SIZE);
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor(
                    Thread.ofPlatform()
                            .name("log-sse-heartbeat")
                            .factory()
            );

    private volatile boolean running = true;
    private Thread dispatcher;

    @PostConstruct
    public void start() {
        dispatcher = Thread.ofVirtual()
                .name("log-sse-dispatcher")
                .start(this::dispatchLoop);
        heartbeatExecutor.scheduleAtFixedRate(
                this::sendHeartbeat,
                HEARTBEAT_SECONDS,
                HEARTBEAT_SECONDS,
                TimeUnit.SECONDS
        );
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (dispatcher != null) {
            dispatcher.interrupt();
        }
        heartbeatExecutor.shutdownNow();

        subscribers.values().forEach(SseEmitter::complete);
        subscribers.clear();
    }

    public SseEmitter subscribe(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        SseEmitter emitter = new SseEmitter(0L);
        SseEmitter previous = subscribers.put(userId, emitter);
        if (previous != null) {
            previous.complete();
        }

        Runnable remove = () -> remove(userId, emitter);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(error -> remove.run());

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException exception) {
            remove.run();
        }

        return emitter;
    }

    public void publish(LogEvent event) {
        if (event == null || event.userId() == null) {
            return;
        }

        // 队列满时丢弃日志，避免阻塞业务线程。
        logQueue.offer(event);
    }

    public void publishUsage(Integer userId) {
        SseEmitter emitter = subscribers.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .name("usage")
                        .data("refresh"));
            }
        } catch (Exception exception) {
            subscribers.remove(userId, emitter);
        }
    }

    private void dispatchLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                broadcast(logQueue.take());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void broadcast(LogEvent event) {
        SseEmitter emitter = subscribers.get(event.userId());
        if (emitter == null) {
            return;
        }

        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(event.id()))
                        .name("log")
                        .data(event));
            }
        } catch (Exception exception) {
            // 使用 remove(key, value)，避免误删同一用户后来建立的新连接。
            subscribers.remove(event.userId(), emitter);
        }
    }

    private void sendHeartbeat() {
        subscribers.forEach((userId, emitter) -> {
            try {
                synchronized (emitter) {
                    emitter.send(SseEmitter.event().comment("ping"));
                }
            } catch (Exception exception) {
                subscribers.remove(userId, emitter);
            }
        });
    }

    private void remove(Integer userId, SseEmitter emitter) {
        subscribers.remove(userId, emitter);
    }
}

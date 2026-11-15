package org.example.wxbot.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.entity.UserTask;
import org.example.wxbot.mapper.UserTaskMapper;
import org.example.wxbot.service.UserTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
public class UserTaskManager implements ApplicationRunner {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private UserTaskMapper userTaskMapper;
    @Autowired
    private UserTaskService userTaskService;
    @Autowired
    @Qualifier("taskScheduler")
    private ThreadPoolTaskScheduler taskScheduler;
    @Autowired
    @Qualifier("botExecutor")
    private ExecutorService botExecutor;

    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    @Override
    public void run(ApplicationArguments args) {
        botExecutor.submit(this::loadEnabledTasks);
    }

    public void refresh(UserTask task) {
        if (task == null || task.getId() == null) {
            log.warn("刷新定时任务失败，任务 ID 为空");
            return;
        }

        cancel(task.getId());
        if (Integer.valueOf(1).equals(task.getStatus())) {
            schedule(task);
        }
    }

    public void cancel(Long taskId) {
        if (taskId == null) {
            return;
        }
        ScheduledFuture<?> future = scheduledTasks.remove(taskId);
        if (future != null) {
            future.cancel(true);
            log.info("已取消定时任务 | taskId={}", taskId);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduledTasks.values().forEach(future -> future.cancel(true));
        scheduledTasks.clear();
    }

    private void loadEnabledTasks() {
        try {
            for (UserTask task : userTaskMapper.selectEnabled()) {
                schedule(task);
            }
            log.info("定时任务加载完成，共 {} 个", scheduledTasks.size());
        } catch (Exception exception) {
            log.error("加载定时任务失败", exception);
        }
    }

    private void schedule(UserTask task) {
        String cron = task.getCronExpression();
        if (cron == null || cron.isBlank()) {
            log.warn("任务 Cron 为空，已跳过 | taskId={}", task.getId());
            return;
        }

        try {
            CronExpression.parse(cron);
            ScheduledFuture<?> future = taskScheduler.schedule(
                    () -> botExecutor.submit(() -> userTaskService.execute(task)),
                    new CronTrigger(cron, ZONE)
            );
            if (future == null) {
                log.warn("任务注册失败 | taskId={} | cron={}", task.getId(), cron);
                return;
            }
            scheduledTasks.put(task.getId(), future);
            log.info("已注册定时任务 | taskId={} | taskName={} | cron={}",
                    task.getId(), task.getTaskName(), cron);
        } catch (IllegalArgumentException exception) {
            log.warn("非法 Cron，任务已跳过 | taskId={} | cron={}",
                    task.getId(), cron, exception);
        }
    }
}

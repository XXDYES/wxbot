package org.example.wxbot.service;

import com.github.wechat.ilink.sdk.ILinkClient;
import com.github.wechat.ilink.sdk.core.login.LoginContext;
import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.entity.UserTask;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.BotClientManager;
import org.example.wxbot.manager.UserTaskManager;
import org.example.wxbot.mapper.UserTaskMapper;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserTaskService {

    @Autowired
    private BotClientManager botClientManager;
    @Autowired
    private AiService aiService;
    @Autowired
    private UserTaskMapper userTaskMapper;
    @Autowired
    @Lazy
    private UserTaskManager userTaskManager;

    public List<UserTask> list(Integer userId) {
        validateUserId(userId);
        return userTaskMapper.selectByUserId(userId);
    }

    public UserTask create(UserTask task, Integer userId) {
        validateUserId(userId);
        if (task == null) {
            throw new BusinessException("任务不能为空");
        }
        task.setId(null);
        task.setUserId(userId);
        task.setCreateAt(null);
        if (task.getStatus() == null) {
            task.setStatus(1);
        }
        validate(task);

        if (userTaskMapper.insert(task) <= 0) {
            throw new BusinessException("定时任务创建失败");
        }
        userTaskManager.refresh(task);
        log.info("定时任务创建成功 | taskId={} | taskName={}", task.getId(), task.getTaskName());
        return task;
    }

    public UserTask update(Long id, UserTask task, Integer userId) {
        validateUserId(userId);
        if (task == null) {
            throw new BusinessException("任务不能为空");
        }
        UserTask existing = getOwnedTask(id, userId);
        task.setId(id);
        task.setUserId(userId);
        task.setCreateAt(existing.getCreateAt());
        if (task.getStatus() == null) {
            task.setStatus(existing.getStatus());
        }
        validate(task);

        if (userTaskMapper.updateById(task) <= 0) {
            throw new BusinessException("定时任务修改失败");
        }
        userTaskManager.refresh(task);
        log.info("定时任务修改成功 | taskId={} | taskName={}", task.getId(), task.getTaskName());
        return task;
    }

    public void updateStatus(Long id, Integer status, Integer userId) {
        validateUserId(userId);
        validateStatus(status);
        UserTask task = getOwnedTask(id, userId);
        task.setStatus(status);

        if (userTaskMapper.updateById(task) <= 0) {
            throw new BusinessException("定时任务状态修改失败");
        }
        userTaskManager.refresh(task);
//        log.info("定时任务状态修改成功 | taskId={} | status={}", id, status);
    }

    public void delete(Long id, Integer userId) {
        validateUserId(userId);
        getOwnedTask(id, userId);
        if (userTaskMapper.deleteById(id) <= 0) {
            throw new BusinessException("定时任务删除失败");
        }
        userTaskManager.cancel(id);
        log.info("定时任务删除成功 | taskId={}", id);
    }

    public void execute(UserTask task) {
        if (task == null || task.getId() == null || task.getUserId() == null) {
            log.warn("定时任务参数无效，已跳过");
            return;
        }

        try (MDC.MDCCloseable ignored =
                     MDC.putCloseable("userId", String.valueOf(task.getUserId()))) {
            executeInternal(task);
        } catch (Exception exception) {
            log.error("定时任务失败 | taskId={} | taskType={}",
                    task.getId(), task.getTaskType(), exception);
        }
    }

    private void executeInternal(UserTask task) throws Exception {
        ILinkClient client = botClientManager.get(task.getUserId());
        if (client == null || !client.isLoggedIn()) {
            log.warn("机器人未登录，任务跳过 | taskId={}", task.getId());
            return;
        }

        LoginContext loginContext = client.getLoginContext();
        if (loginContext == null
                || loginContext.getUserId() == null
                || loginContext.getUserId().isBlank()) {
            log.warn("机器人登录上下文无效，任务跳过 | taskId={}", task.getId());
            return;
        }

        String targetUserId = loginContext.getUserId();
        log.info("定时任务开始 | taskId={} | taskName={} | taskType={}",
                task.getId(), task.getTaskName(), task.getTaskType());

        if (Integer.valueOf(1).equals(task.getTaskType())) {
            client.sendText(targetUserId, task.getContent());
        } else if (Integer.valueOf(2).equals(task.getTaskType())) {
            aiService.aiResponse(
                    task.getUserId(),
                    task.getContent(),
                    null,
                    reply -> {
                        try {
                            client.sendText(targetUserId, reply);
                        } catch (Exception exception) {
                            throw new RuntimeException("发送定时任务 AI 回复失败", exception);
                        }
                    }
            );
        } else {
            log.warn("未知任务类型，任务跳过 | taskId={} | taskType={}",
                    task.getId(), task.getTaskType());
            return;
        }

        log.info("定时任务完成 | taskId={}", task.getId());
    }

    private UserTask getOwnedTask(Long id, Integer userId) {
        UserTask task = id == null ? null : userTaskMapper.selectById(id);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new BusinessException("定时任务不存在");
        }
        return task;
    }

    private void validate(UserTask task) {
        if (task.getTaskName() == null || task.getTaskName().isBlank()) {
            throw new BusinessException("任务名称不能为空");
        }
        if (task.getTaskName().length() > 50) {
            throw new BusinessException("任务名称不能超过50个字");
        }
        if (!Integer.valueOf(1).equals(task.getTaskType())
                && !Integer.valueOf(2).equals(task.getTaskType())) {
            throw new BusinessException("任务类型只能是1或2");
        }
        if (task.getContent() == null || task.getContent().isBlank()) {
            throw new BusinessException("任务内容不能为空");
        }
        if (task.getCronExpression() == null || task.getCronExpression().isBlank()) {
            throw new BusinessException("Cron 表达式不能为空");
        }
        try {
            CronExpression.parse(task.getCronExpression());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("Cron 表达式格式错误");
        }
        validateStatus(task.getStatus());
    }

    private void validateStatus(Integer status) {
        if (!Integer.valueOf(0).equals(status) && !Integer.valueOf(1).equals(status)) {
            throw new BusinessException("任务状态只能是0或1");
        }
    }

    private void validateUserId(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户未登录");
        }
    }
}

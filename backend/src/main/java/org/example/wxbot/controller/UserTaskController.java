package org.example.wxbot.controller;

import org.example.wxbot.common.Result;
import org.example.wxbot.common.UserContext;
import org.example.wxbot.entity.UserTask;
import org.example.wxbot.service.UserTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class UserTaskController {

    @Autowired
    private UserTaskService userTaskService;

    @GetMapping
    public Result<List<UserTask>> list() {
        return Result.success(userTaskService.list(UserContext.getCurrentId()));
    }

    @PostMapping
    public Result<UserTask> create(@RequestBody UserTask task) {
        return Result.success(userTaskService.create(task, UserContext.getCurrentId()));
    }

    @PutMapping("/{id}")
    public Result<UserTask> update(@PathVariable Long id, @RequestBody UserTask task) {
        return Result.success(userTaskService.update(
                id, task, UserContext.getCurrentId()
        ));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody UserTask request
    ) {
        userTaskService.updateStatus(
                id, request.getStatus(), UserContext.getCurrentId()
        );
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userTaskService.delete(id, UserContext.getCurrentId());
        return Result.success();
    }
}

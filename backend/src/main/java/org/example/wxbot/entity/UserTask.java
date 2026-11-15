package org.example.wxbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("user_task")
public class UserTask {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Integer taskType;
    private String taskName;
    private String cronExpression;
    private Integer status;
    private String content;
    private LocalDate createAt;
}

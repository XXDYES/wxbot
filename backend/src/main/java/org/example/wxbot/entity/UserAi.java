package org.example.wxbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_ai")
public class UserAi {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String model;
    private String reasoningEffort;
    private String persona;
}

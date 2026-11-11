package org.example.wxbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("user_bot")
public class UserBot {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private String ilinkUserId;
    private String botId;
    private String botToken;
    private String baseUrl;
    private LocalDate createAt;
}

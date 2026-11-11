package org.example.wxbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.example.wxbot.entity.UserBot;

public interface UserBotMapper extends BaseMapper<UserBot> {

    @Select("SELECT * FROM user_bot WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
    UserBot findLatestByUserId(Integer userId);

    @Delete("DELETE FROM user_bot WHERE user_id = #{userId}")
    int deleteByUserId(Integer userId);
}

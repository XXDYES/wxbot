package org.example.wxbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.wxbot.entity.UserAi;

public interface UserAiMapper extends BaseMapper<UserAi> {
    @Select("SELECT * FROM user_ai WHERE user_id = #{userId}")
    UserAi getAiSetting(Integer userId);
}

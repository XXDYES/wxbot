package org.example.wxbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.wxbot.entity.User;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT id, username, password FROM users WHERE username = #{username}")
    User selectByUsername(String username);
}

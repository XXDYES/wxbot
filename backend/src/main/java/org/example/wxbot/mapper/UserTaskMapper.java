package org.example.wxbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.wxbot.entity.UserTask;

import java.util.List;

public interface UserTaskMapper extends BaseMapper<UserTask> {

    @Select("SELECT * FROM user_task WHERE user_id = #{userId} ORDER BY id DESC")
    List<UserTask> selectByUserId(Integer userId);

    @Select("SELECT * FROM user_task WHERE status = 1")
    List<UserTask> selectEnabled();
}

package org.example.wxbot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.wxbot.entity.UserLibrary;

import java.util.List;

public interface UserLibraryMapper extends BaseMapper<UserLibrary> {

    @Select("SELECT * FROM user_library WHERE user_id = #{userId} ORDER BY id DESC")
    List<UserLibrary> selectByUserId(Integer userId);
}

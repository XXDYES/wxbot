package org.example.wxbot.service;

import jakarta.annotation.Resource;
import org.example.wxbot.common.EncodeUtil;
import org.example.wxbot.entity.Register;
import org.example.wxbot.entity.User;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    public User login(User user) {
        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new BusinessException("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }

        User dbUser = userMapper.selectByUsername(user.getUsername());
        if (dbUser == null) {
            throw new BusinessException("用户名不存在");
        }
        if (!EncodeUtil.matches(user.getPassword(), dbUser.getPassword())) {
            throw new BusinessException("密码错误");
        }

        return dbUser;
    }

    public void register(Register register) {
        if (register.getUsername() == null || register.getUsername().isBlank()) {
            throw new BusinessException("账号不能为空");
        }
        if (register.getUsername().length() > 10) {
            throw new BusinessException("用户名长度不能超过10个字符");
        }
        if (register.getPassword() == null || register.getPassword().isBlank()) {
            throw new BusinessException("密码不能为空");
        }
        if (register.getPassword().length() > 15) {
            throw new BusinessException("密码不能超过15个字符");
        }
        if (register.getRepassword() == null || register.getRepassword().isBlank()) {
            throw new BusinessException("需要再次输入密码");
        }
        if (!register.getPassword().equals(register.getRepassword())) {
            throw new BusinessException("密码不相同");
        }
        if (userMapper.selectByUsername(register.getUsername()) != null) {
            throw new BusinessException("用户名已经存在");
        }

        User user = new User();
        user.setUsername(register.getUsername());
        user.setPassword(EncodeUtil.encode(register.getPassword()));
        if (userMapper.insert(user) <= 0) {
            throw new BusinessException("注册失败，数据库插入异常");
        }
    }

    public User getUserById(Integer id) {
        return userMapper.selectById(id);
    }
}

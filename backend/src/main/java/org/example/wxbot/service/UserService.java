package org.example.wxbot.service;

import jakarta.annotation.Resource;
import org.example.wxbot.entity.Register;
import org.example.wxbot.entity.User;
import org.example.wxbot.entity.UserAi;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.UserAiManager;
import org.example.wxbot.mapper.UserAiMapper;
import org.example.wxbot.mapper.UserMapper;
import org.example.wxbot.util.EncodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;
    @Autowired
    private UserAiMapper userAiMapper;
    @Autowired
    private UserAiManager userAiManager;

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
        }else{
            userAiManager.createDefault(user.getId());
        }
    }

    public User getUserById(Integer id) {
        return userMapper.selectById(id);
    }
    public UserAi getAiSetting(Integer userId){
        return userAiManager.get(userId);
    }
    public void setPersona(Integer userId,String persona){
        if (persona == null) {
            throw new BusinessException("人设不能为空");
        }
        if (persona.length() > 300) {
            throw new BusinessException("人设不能超过300个字符");
        }

        UserAi current = userAiManager.get(userId);
        UserAi update = new UserAi();
        update.setId(current.getId());
        update.setPersona(persona);
        if (userAiMapper.updateById(update) <= 0) {
            throw new BusinessException("人设修改失败");
        }

        current.setPersona(persona);
        userAiManager.put(current);
    }
}

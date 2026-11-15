package org.example.wxbot.service;

import jakarta.annotation.Resource;
import org.example.wxbot.entity.*;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.manager.LibraryManager;
import org.example.wxbot.manager.UserAiManager;
import org.example.wxbot.mapper.UserAiMapper;
import org.example.wxbot.mapper.UserLibraryMapper;
import org.example.wxbot.mapper.UserMapper;
import org.example.wxbot.mapper.UserTaskMapper;
import org.example.wxbot.util.EncodeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    @Resource
    private UserMapper userMapper;
    @Autowired
    private UserAiMapper userAiMapper;
    @Autowired
    private UserAiManager userAiManager;
    @Autowired
    private UserLibraryMapper userLibraryMapper;
    @Autowired
    private LibraryManager libraryManager;
    @Autowired
    private UserTaskMapper userTaskMapper;

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

    @Transactional(rollbackFor = Exception.class)
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
            UserLibrary userLibrary = new UserLibrary();
            userLibrary.setUserId(user.getId());
            userLibrary.setTitle("原神牛逼");
            userLibrary.setContent("你说得对，但是《原神》是由米哈游自主研发的一款全新开放世界冒险游戏。" +
            "游戏发生在一个被称作“提瓦特”的幻想世界，在这里，被神选中的人将被授予“神之眼”，导引元素之力。" +
            "你将扮演一位名为“旅行者”的神秘角色，在自由的旅行中邂逅性格各异、能力独特的同伴们，和他们一起击败强敌，" +
            "找回失散的亲人——同时，逐步发掘“原神”的真相。");
            userLibraryMapper.insert(userLibrary);
            UserTask userTask = new UserTask();
            userTask.setUserId(user.getId());
            userTask.setTaskType(2);
            userTask.setTaskName("A股大盘");
            userTask.setCronExpression("0 0 22 * * *");
            userTask.setStatus(0);
            userTask.setContent("分析今天的A股大盘走势");
            userTaskMapper.insert(userTask);

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
    public List<UserLibrary> getLibrary(Integer userId){
        return userLibraryMapper.selectByUserId(userId);
    }
    public void addLibrary(UserLibrary userLibrary){
        validateLibrary(userLibrary, null);
        userLibraryMapper.insert(userLibrary);
        libraryManager.put(userLibrary);
    }

    public void updateLibrary(UserLibrary userLibrary, Integer userId) {
        getOwnedLibrary(userLibrary.getId(), userId);
        userLibrary.setUserId(userId);
        validateLibrary(userLibrary, userLibrary.getId());
        if (userLibraryMapper.updateById(userLibrary) <= 0) {
            throw new BusinessException("知识库修改失败");
        }
        libraryManager.put(userLibrary);
    }

    public void deleteLibrary(Integer id, Integer userId) {
        getOwnedLibrary(id, userId);
        if (userLibraryMapper.deleteById(id) <= 0) {
            throw new BusinessException("知识库删除失败");
        }
        libraryManager.remove(userId, id);
    }

    private UserLibrary getOwnedLibrary(Integer id, Integer userId) {
        UserLibrary library = id == null ? null : userLibraryMapper.selectById(id);
        if (library == null || !userId.equals(library.getUserId())) {
            throw new BusinessException("知识库不存在");
        }
        return library;
    }

    private void validateLibrary(UserLibrary userLibrary, Integer excludeId) {
        if (userLibrary.getTitle() == null || userLibrary.getTitle().isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        if (userLibrary.getTitle().length() > 15) {
            throw new BusinessException("标题不能超过15个字");
        }
        if (userLibrary.getDescription() != null
                && userLibrary.getDescription().length() > 50) {
            throw new BusinessException("描述不能超过50个字");
        }
        if (userLibrary.getContent() == null || userLibrary.getContent().isBlank()) {
            throw new BusinessException("内容不能为空");
        }

        long existingLength = userLibraryMapper.selectByUserId(userLibrary.getUserId())
                .stream()
                .filter(item -> excludeId == null || !excludeId.equals(item.getId()))
                .mapToLong(item -> item.getContent() == null ? 0 : item.getContent().length())
                .sum();
        if (existingLength + userLibrary.getContent().length() > 10_000) {
            throw new BusinessException("知识库内容总字数不能超过10000字");
        }
    }
}

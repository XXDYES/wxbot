package org.example.wxbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.common.Result;
import org.example.wxbot.common.UserContext;
import org.example.wxbot.entity.Register;
import org.example.wxbot.entity.User;
import org.example.wxbot.entity.UserAi;
import org.example.wxbot.entity.UserLibrary;
import org.example.wxbot.service.AiService;
import org.example.wxbot.service.UserService;
import org.example.wxbot.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import java.util.Map;

@RestController
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private AiService aiService;
    @PostMapping("/login")
    public Result<String> login(@RequestBody User user){
        User dbUser = userService.login(user);
        log.info(String.format("%s登录了",dbUser.getUsername()));
        String token = jwtUtil.generateToken(dbUser);
        return Result.success(token);
    }
    @PostMapping("/register")
    public Result register(@RequestBody Register register){
        userService.register(register);
        return Result.success("注册成功");
    }
    @GetMapping("/userInfo")
    public Result<User> getUserInfo() {
        Integer userId = UserContext.getCurrentId();
        String username = UserContext.getCurrentUsername();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        User user = new User();
        user.setId(userId);
        user.setUsername(username);
        return Result.success(user);
    }

    @GetMapping("/getAiSetting")
    public Result<UserAi> getAiSetting(){
        Integer userId = UserContext.getCurrentId();
        UserAi userAi = userService.getAiSetting(userId);
        return Result.success(userAi);
    }

    @GetMapping("/getUsage")
    public Result<Map<String, Long>> getUsage(){
        return Result.success(aiService.getUsage(UserContext.getCurrentId()));
    }

    @PutMapping("/setPersona")
    public Result<Void> setPersona(@RequestBody Map<String, String> request){
        Integer userId = UserContext.getCurrentId();
        userService.setPersona(userId, request.get("persona"));
        return Result.success();
    }
    @GetMapping("getLibrary")
    public Result<List<UserLibrary>> getLibrary(){
        Integer userId = UserContext.getCurrentId();
        return Result.success(userService.getLibrary(userId));
    }
    @PostMapping("/addLibrary")
    public Result addLibray(@RequestBody UserLibrary userLibrary){
        userLibrary.setUserId(UserContext.getCurrentId());
        userService.addLibrary(userLibrary);
        return Result.success();
    }

    @PutMapping("/updateLibrary")
    public Result updateLibrary(@RequestBody UserLibrary userLibrary){
        userService.updateLibrary(userLibrary, UserContext.getCurrentId());
        return Result.success();
    }

    @DeleteMapping("/deleteLibrary/{id}")
    public Result deleteLibrary(@PathVariable Integer id){
        userService.deleteLibrary(id, UserContext.getCurrentId());
        return Result.success();
    }
}

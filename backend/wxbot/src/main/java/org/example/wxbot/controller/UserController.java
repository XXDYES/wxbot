package org.example.wxbot.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.common.JwtUtil;
import org.example.wxbot.common.Result;
import org.example.wxbot.entity.Register;
import org.example.wxbot.entity.User;
import org.example.wxbot.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;
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
}

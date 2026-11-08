package org.example.wxbot.entity;

import lombok.Data;

@Data
public class Register {
    private String username;
    private String password;
    private String repassword;
}

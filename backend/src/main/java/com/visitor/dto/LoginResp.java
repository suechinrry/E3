package com.visitor.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResp {
    @Schema(description = "JWT token")
    private String token;

    @Schema(description = "用户信息")
    private UserInfo user;

    @Data
    @AllArgsConstructor
    public static class UserInfo {
        private Integer id;
        private String username;
        private String name;
        private String role;
        private String phone;
        private String department;
        private String company;
    }
}

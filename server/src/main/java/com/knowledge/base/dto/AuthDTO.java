package com.knowledge.base.dto;

import lombok.Data;

public class AuthDTO {

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String nickname;
    }

    @Data
    public static class TokenResponse {
        private String token;
        private UserDTO user;
    }
}

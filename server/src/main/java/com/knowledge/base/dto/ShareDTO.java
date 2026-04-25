package com.knowledge.base.dto;

import lombok.Data;

public class ShareDTO {

    @Data
    public static class CreateRequest {
        private Long documentId;
        private String type;
        private String password;
        private Integer expiresInHours;
    }

    @Data
    public static class AccessRequest {
        private String shareCode;
        private String password;
    }

    @Data
    public static class Response {
        private Long id;
        private Long documentId;
        private String shareCode;
        private String type;
        private boolean isEnabled;
        private String expiresAt;
        private boolean hasPassword;
        private String createdAt;
    }
}

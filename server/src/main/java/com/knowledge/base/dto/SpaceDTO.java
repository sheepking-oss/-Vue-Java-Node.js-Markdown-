package com.knowledge.base.dto;

import lombok.Data;

public class SpaceDTO {

    @Data
    public static class CreateRequest {
        private String name;
        private String description;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private String description;
    }

    @Data
    public static class AddMemberRequest {
        private Long userId;
        private String role;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String description;
        private UserDTO owner;
        private String createdAt;
        private String updatedAt;
    }
}

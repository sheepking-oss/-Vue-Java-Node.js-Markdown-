package com.knowledge.base.dto;

import lombok.Data;

public class CommentDTO {

    @Data
    public static class CreateRequest {
        private Long documentId;
        private Long parentId;
        private String content;
    }

    @Data
    public static class UpdateRequest {
        private String content;
    }

    @Data
    public static class Response {
        private Long id;
        private Long documentId;
        private Long parentId;
        private String content;
        private UserDTO user;
        private String createdAt;
        private String updatedAt;
    }
}

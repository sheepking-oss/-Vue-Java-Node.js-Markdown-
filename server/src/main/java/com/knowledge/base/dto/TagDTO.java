package com.knowledge.base.dto;

import lombok.Data;

public class TagDTO {

    @Data
    public static class CreateRequest {
        private String name;
        private String color;
    }

    @Data
    public static class Response {
        private Long id;
        private String name;
        private String color;
        private Long spaceId;
    }
}

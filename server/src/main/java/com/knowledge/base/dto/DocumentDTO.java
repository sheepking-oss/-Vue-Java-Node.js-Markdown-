package com.knowledge.base.dto;

import lombok.Data;

import java.util.Set;

public class DocumentDTO {

    @Data
    public static class CreateRequest {
        private String title;
        private String content;
        private Long spaceId;
        private Long parentId;
    }

    @Data
    public static class UpdateRequest {
        private String title;
        private String content;
        private Long parentId;
        private String changeNote;
    }

    @Data
    public static class AutoSaveRequest {
        private String draftContent;
    }

    @Data
    public static class SearchRequest {
        private String keyword;
        private Set<Long> tagIds;
    }

    @Data
    public static class Response {
        private Long id;
        private String title;
        private String content;
        private String draftContent;
        private Long parentId;
        private Long spaceId;
        private Set<TagDTO> tags;
        private UserDTO createdBy;
        private UserDTO updatedBy;
        private Integer version;
        private boolean isDeleted;
        private String createdAt;
        private String updatedAt;
        private String autoSavedAt;
    }
}

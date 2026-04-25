package com.knowledge.base.controller;

import com.knowledge.base.dto.CommentDTO;
import com.knowledge.base.dto.UserDTO;
import com.knowledge.base.entity.Comment;
import com.knowledge.base.entity.Document;
import com.knowledge.base.security.UserPrincipal;
import com.knowledge.base.service.CommentService;
import com.knowledge.base.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final DocumentService documentService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @PostMapping
    public ResponseEntity<?> createComment(
            @RequestBody CommentDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Comment comment = commentService.createComment(
                request.getDocumentId(),
                request.getParentId(),
                request.getContent(),
                currentUser
        );
        return ResponseEntity.ok(convertToDTO(comment));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateComment(
            @PathVariable Long id,
            @RequestBody CommentDTO.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Comment comment = commentService.updateComment(id, request.getContent(), currentUser);
        return ResponseEntity.ok(convertToDTO(comment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        commentService.deleteComment(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<?> getDocumentComments(@PathVariable Long documentId) {
        Document document = documentService.getDocument(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        
        List<Comment> comments = commentService.getDocumentComments(document);
        return ResponseEntity.ok(comments.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/{commentId}/replies")
    public ResponseEntity<?> getCommentReplies(@PathVariable Long commentId) {
        Comment parent = commentService.getComment(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));
        
        List<Comment> replies = commentService.getCommentReplies(parent);
        return ResponseEntity.ok(replies.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    private CommentDTO.Response convertToDTO(Comment comment) {
        CommentDTO.Response dto = new CommentDTO.Response();
        dto.setId(comment.getId());
        dto.setDocumentId(comment.getDocument() != null ? comment.getDocument().getId() : null);
        dto.setParentId(comment.getParent() != null ? comment.getParent().getId() : null);
        dto.setContent(comment.getContent());
        
        if (comment.getUser() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(comment.getUser().getId());
            userDTO.setUsername(comment.getUser().getUsername());
            userDTO.setAvatar(comment.getUser().getAvatar());
            userDTO.setNickname(comment.getUser().getNickname());
            dto.setUser(userDTO);
        }
        
        dto.setCreatedAt(comment.getCreatedAt() != null ? comment.getCreatedAt().format(dateTimeFormatter) : null);
        dto.setUpdatedAt(comment.getUpdatedAt() != null ? comment.getUpdatedAt().format(dateTimeFormatter) : null);
        
        return dto;
    }
}

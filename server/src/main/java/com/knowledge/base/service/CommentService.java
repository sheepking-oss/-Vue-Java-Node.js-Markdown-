package com.knowledge.base.service;

import com.knowledge.base.entity.Comment;
import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.User;
import com.knowledge.base.repository.CommentRepository;
import com.knowledge.base.repository.DocumentRepository;
import com.knowledge.base.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;

    @Transactional
    public Comment createComment(Long documentId, Long parentId, String content, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!documentService.hasViewPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to access document");
        }

        Comment comment = new Comment();
        comment.setDocument(document);
        comment.setContent(content);
        comment.setUser(currentUser.getUser());

        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("Parent comment not found"));
            if (!parent.getDocument().getId().equals(documentId)) {
                throw new IllegalArgumentException("Parent comment belongs to different document");
            }
            comment.setParent(parent);
        }

        return commentRepository.save(comment);
    }

    @Transactional
    public Comment updateComment(Long commentId, String content, UserPrincipal currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        if (!comment.getUser().getId().equals(currentUser.getUser().getId())) {
            throw new SecurityException("No permission to update this comment");
        }

        comment.setContent(content);
        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, UserPrincipal currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found"));

        boolean isOwner = comment.getUser().getId().equals(currentUser.getUser().getId());
        boolean isSpaceAdmin = documentService.hasViewPermission(comment.getDocument().getSpace(), currentUser.getUser()) 
                && isSpaceAdmin(comment.getDocument().getSpace(), currentUser.getUser());

        if (!isOwner && !isSpaceAdmin) {
            throw new SecurityException("No permission to delete this comment");
        }

        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

    public Optional<Comment> getComment(Long commentId) {
        return commentRepository.findById(commentId);
    }

    public List<Comment> getDocumentComments(Document document) {
        return commentRepository.findByDocumentAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(document);
    }

    public List<Comment> getCommentReplies(Comment parent) {
        return commentRepository.findByParentAndIsDeletedFalseOrderByCreatedAtAsc(parent);
    }

    private boolean isSpaceAdmin(com.knowledge.base.entity.Space space, User user) {
        return false;
    }
}

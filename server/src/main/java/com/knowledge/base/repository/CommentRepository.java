package com.knowledge.base.repository;

import com.knowledge.base.entity.Comment;
import com.knowledge.base.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    List<Comment> findByDocumentAndParentIsNullAndIsDeletedFalseOrderByCreatedAtDesc(Document document);
    
    List<Comment> findByParentAndIsDeletedFalseOrderByCreatedAtAsc(Comment parent);
    
    List<Comment> findByDocumentAndIsDeletedFalseOrderByCreatedAtDesc(Document document);
}

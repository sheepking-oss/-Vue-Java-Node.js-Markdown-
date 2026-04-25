package com.knowledge.base.repository;

import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.Space;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    List<Document> findBySpaceAndIsDeletedFalseOrderByUpdatedAtDesc(Space space);
    
    List<Document> findBySpaceAndParentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(Space space);
    
    List<Document> findByParentAndIsDeletedFalseOrderByCreatedAtAsc(Document parent);
    
    Page<Document> findBySpaceAndIsDeletedFalse(Space space, Pageable pageable);
    
    List<Document> findBySpaceAndIsDeletedTrueOrderByDeletedAtDesc(Space space);
    
    Optional<Document> findByIdAndIsDeletedFalse(Long id);
    
    Optional<Document> findByIdAndIsDeletedTrue(Long id);
    
    @Query("SELECT d FROM Document d WHERE d.space = :space AND d.isDeleted = false " +
           "AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(d.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Document> searchByKeyword(@Param("space") Space space, @Param("keyword") String keyword);
    
    boolean existsByTitleAndSpaceAndIsDeletedFalse(String title, Space space);
}

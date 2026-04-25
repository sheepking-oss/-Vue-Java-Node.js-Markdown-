package com.knowledge.base.repository;

import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, Long> {
    
    List<DocumentVersion> findByDocumentOrderByVersionDesc(Document document);
    
    Optional<DocumentVersion> findByDocumentAndVersion(Document document, Integer version);
    
    Integer countByDocument(Document document);
}

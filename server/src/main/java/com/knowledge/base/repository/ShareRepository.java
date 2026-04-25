package com.knowledge.base.repository;

import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.Share;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareRepository extends JpaRepository<Share, Long> {
    
    Optional<Share> findByShareCode(String shareCode);
    
    List<Share> findByDocument(Document document);
    
    Optional<Share> findByShareCodeAndIsEnabledTrue(String shareCode);
}

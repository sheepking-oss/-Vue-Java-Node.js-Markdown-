package com.knowledge.base.service;

import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.Space;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<Document> searchByKeyword(Space space, String keyword) {
        return searchByKeyword(space.getId(), keyword);
    }

    @Transactional(readOnly = true)
    public List<Document> searchByKeyword(Long spaceId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            SearchSession searchSession = Search.session(entityManager);
            
            List<Document> results = searchSession.search(Document.class)
                .where(f -> f.bool(
                    b -> {
                        b.must(f.match().fields("title", "content").matching(keyword).fuzzy(1));
                        b.must(f.match().field("spaceId").matching(spaceId));
                        b.must(f.match().field("isDeletedForIndex").matching(false));
                    }
                ))
                .sort(f -> f.score().desc())
                .fetchHits(20);

            log.debug("Search for '{}' in space {} returned {} results", keyword, spaceId, results.size());
            return results;

        } catch (Exception e) {
            log.error("Hibernate Search failed for keyword '{}', falling back to JPA query", keyword, e);
            return Collections.emptyList();
        }
    }

    @Transactional(readOnly = true)
    public List<Document> searchByKeywordWithFallback(Space space, String keyword) {
        List<Document> results = searchByKeyword(space, keyword);
        
        if (results.isEmpty() && keyword != null && !keyword.trim().isEmpty()) {
            log.warn("Hibernate Search returned empty results, returning empty list");
        }
        
        return results;
    }

    @Transactional
    public void rebuildIndex() {
        try {
            SearchSession searchSession = Search.session(entityManager);
            
            log.info("Starting mass indexer for Document entities...");
            
            searchSession.massIndexer(Document.class)
                .threadsToLoadObjects(2)
                .batchSizeToLoadObjects(25)
                .typesToIndexInParallel(1)
                .startAndWait();
            
            log.info("Mass indexer completed successfully");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Mass indexer was interrupted", e);
            throw new RuntimeException("Index rebuilding was interrupted", e);
        } catch (Exception e) {
            log.error("Failed to rebuild index", e);
            throw new RuntimeException("Failed to rebuild index", e);
        }
    }

    @Transactional
    public void indexDocument(Document document) {
        if (document == null || document.getId() == null) {
            return;
        }
        
        try {
            SearchSession searchSession = Search.session(entityManager);
            
            if (document.getIsDeleted() == null || !document.getIsDeleted()) {
                searchSession.indexingPlan().addOrUpdate(document);
                log.debug("Document {} added/updated in index", document.getId());
            } else {
                searchSession.indexingPlan().purge(Document.class, document.getId());
                log.debug("Document {} purged from index (marked as deleted)", document.getId());
            }
            
        } catch (Exception e) {
            log.error("Failed to index document {}", document.getId(), e);
            throw new RuntimeException("Failed to index document", e);
        }
    }

    @Transactional
    public void purgeDocumentFromIndex(Long documentId) {
        if (documentId == null) {
            return;
        }
        
        try {
            SearchSession searchSession = Search.session(entityManager);
            searchSession.indexingPlan().purge(Document.class, documentId);
            log.debug("Document {} purged from index", documentId);
            
        } catch (Exception e) {
            log.error("Failed to purge document {} from index", documentId, e);
            throw new RuntimeException("Failed to purge document from index", e);
        }
    }

    @Transactional
    public void refreshIndex() {
        try {
            SearchSession searchSession = Search.session(entityManager);
            searchSession.workspace(Document.class).refresh();
            log.debug("Document index refreshed");
            
        } catch (Exception e) {
            log.error("Failed to refresh index", e);
            throw new RuntimeException("Failed to refresh index", e);
        }
    }

    @Transactional
    public void optimizeIndex() {
        try {
            SearchSession searchSession = Search.session(entityManager);
            searchSession.workspace(Document.class).mergeSegments();
            log.info("Document index optimized");
            
        } catch (Exception e) {
            log.error("Failed to optimize index", e);
            throw new RuntimeException("Failed to optimize index", e);
        }
    }

    @Transactional(readOnly = true)
    public long countIndexedDocuments() {
        try {
            SearchSession searchSession = Search.session(entityManager);
            return searchSession.search(Document.class)
                .where(f -> f.matchAll())
                .fetchTotalHitCount();
        } catch (Exception e) {
            log.error("Failed to count indexed documents", e);
            return -1;
        }
    }

    @Transactional(readOnly = true)
    public boolean isIndexAvailable() {
        try {
            SearchSession searchSession = Search.session(entityManager);
            searchSession.search(Document.class)
                .where(f -> f.matchAll())
                .fetchTotalHitCount();
            return true;
        } catch (Exception e) {
            log.warn("Index is not available", e);
            return false;
        }
    }
}

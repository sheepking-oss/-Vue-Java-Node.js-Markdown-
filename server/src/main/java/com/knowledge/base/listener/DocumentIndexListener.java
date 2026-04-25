package com.knowledge.base.listener;

import com.knowledge.base.entity.Document;
import com.knowledge.base.service.IndexSyncService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentIndexListener {

    private final ApplicationContext applicationContext;

    private IndexSyncService getIndexSyncService() {
        return applicationContext.getBean(IndexSyncService.class);
    }

    @PostPersist
    public void onPostPersist(Document document) {
        log.debug("Document {} persisted, triggering index", document.getId());
        try {
            IndexSyncService service = getIndexSyncService();
            service.onDocumentCreated(document);
        } catch (Exception e) {
            log.warn("Failed to trigger index after persist for document {}", document.getId(), e);
        }
    }

    @PostUpdate
    public void onPostUpdate(Document document) {
        log.debug("Document {} updated, triggering index", document.getId());
        try {
            IndexSyncService service = getIndexSyncService();
            
            if (Boolean.TRUE.equals(document.getIsDeleted())) {
                service.onDocumentMovedToTrash(document);
            } else {
                service.onDocumentUpdated(document);
            }
        } catch (Exception e) {
            log.warn("Failed to trigger index after update for document {}", document.getId(), e);
        }
    }

    @PostRemove
    public void onPostRemove(Document document) {
        log.debug("Document {} removed, triggering index purge", document.getId());
        try {
            IndexSyncService service = getIndexSyncService();
            service.onDocumentDeleted(document);
        } catch (Exception e) {
            log.warn("Failed to trigger index purge after remove for document {}", document.getId(), e);
        }
    }
}

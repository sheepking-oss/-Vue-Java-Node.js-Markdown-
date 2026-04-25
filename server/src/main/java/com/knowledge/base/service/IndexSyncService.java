package com.knowledge.base.service;

import com.knowledge.base.entity.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexSyncService {

    private final SearchService searchService;
    private final ApplicationEventPublisher eventPublisher;
    private final IndexRetryService indexRetryService;

    private final ConcurrentLinkedQueue<IndexOperation> pendingOperations = new ConcurrentLinkedQueue<>();
    private final AtomicInteger pendingCount = new AtomicInteger(0);

    public void onDocumentCreated(Document document) {
        log.debug("Document {} created, scheduling index update", document.getId());
        publishEvent(document, IndexOperation.Type.CREATE);
    }

    public void onDocumentUpdated(Document document) {
        log.debug("Document {} updated, scheduling index update", document.getId());
        publishEvent(document, IndexOperation.Type.UPDATE);
    }

    public void onDocumentDeleted(Document document) {
        log.debug("Document {} deleted, scheduling index purge", document.getId());
        publishEvent(document, IndexOperation.Type.DELETE);
    }

    public void onDocumentMovedToTrash(Document document) {
        log.debug("Document {} moved to trash, scheduling index purge", document.getId());
        publishEvent(document, IndexOperation.Type.DELETE);
    }

    public void onDocumentRestoredFromTrash(Document document) {
        log.debug("Document {} restored from trash, scheduling index update", document.getId());
        publishEvent(document, IndexOperation.Type.RESTORE);
    }

    public void onRollback(Document document) {
        log.debug("Document {} rolled back, scheduling index update", document.getId());
        publishEvent(document, IndexOperation.Type.UPDATE);
    }

    private void publishEvent(Document document, IndexOperation.Type type) {
        IndexOperation operation = new IndexOperation(
            document.getId(),
            type,
            document.getTitle(),
            document.getContent(),
            document.getIsDeleted()
        );
        
        eventPublisher.publishEvent(operation);
        
        pendingOperations.offer(operation);
        pendingCount.incrementAndGet();
        
        log.debug("Published index operation for document {}: {}", document.getId(), type);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleIndexOperation(IndexOperation operation) {
        log.debug("Handling index operation after commit: documentId={}, type={}", 
            operation.documentId(), operation.type());
        
        try {
            processIndexOperation(operation);
            pendingOperations.remove(operation);
            pendingCount.decrementAndGet();
            
        } catch (Exception e) {
            log.error("Failed to process index operation for document {}", operation.documentId(), e);
            
            indexRetryService.scheduleRetry(operation, e);
            
            pendingOperations.remove(operation);
            pendingCount.decrementAndGet();
        }
    }

    private void processIndexOperation(IndexOperation operation) {
        switch (operation.type()) {
            case CREATE, UPDATE, RESTORE -> {
                searchService.refreshIndex();
                log.info("Index refreshed for document {} ({})", operation.documentId(), operation.type());
            }
            case DELETE -> {
                searchService.purgeDocumentFromIndex(operation.documentId());
                searchService.refreshIndex();
                log.info("Document {} purged from index", operation.documentId());
            }
        }
    }

    public int getPendingOperationCount() {
        return pendingCount.get();
    }

    public void clearPendingOperations() {
        pendingOperations.clear();
        pendingCount.set(0);
    }

    public record IndexOperation(
        Long documentId,
        Type type,
        String title,
        String content,
        Boolean isDeleted
    ) {
        public enum Type {
            CREATE, UPDATE, DELETE, RESTORE
        }
    }
}

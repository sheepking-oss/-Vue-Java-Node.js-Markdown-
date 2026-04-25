package com.knowledge.base.service;

import com.knowledge.base.service.IndexSyncService.IndexOperation;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexRetryService {

    private final SearchService searchService;
    private final TaskScheduler taskScheduler;

    private final Map<Long, RetryInfo> retryQueue = new ConcurrentHashMap<>();
    private final Map<Long, ScheduledFuture<?>> scheduledRetries = new ConcurrentHashMap<>();

    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long[] RETRY_DELAYS = {1000, 2000, 5000, 10000, 30000};

    public void scheduleRetry(IndexOperation operation, Exception cause) {
        Long documentId = operation.documentId();
        
        RetryInfo existing = retryQueue.get(documentId);
        int attempt = (existing != null) ? existing.attempt() + 1 : 1;

        if (attempt > MAX_RETRY_ATTEMPTS) {
            log.error("Max retry attempts ({}) reached for document {}, giving up", 
                MAX_RETRY_ATTEMPTS, documentId);
            retryQueue.remove(documentId);
            cancelScheduledRetry(documentId);
            return;
        }

        RetryInfo retryInfo = new RetryInfo(operation, attempt, cause);
        retryQueue.put(documentId, retryInfo);

        long delay = RETRY_DELAYS[Math.min(attempt - 1, RETRY_DELAYS.length - 1)];
        
        log.warn("Scheduling retry {} for document {} in {}ms", 
            attempt, documentId, delay);

        cancelScheduledRetry(documentId);

        ScheduledFuture<?> future = taskScheduler.schedule(
            () -> executeRetry(documentId),
            Instant.now().plusMillis(delay)
        );
        scheduledRetries.put(documentId, future);
    }

    private void executeRetry(Long documentId) {
        RetryInfo retryInfo = retryQueue.get(documentId);
        if (retryInfo == null) {
            log.debug("No retry info found for document {}", documentId);
            return;
        }

        log.info("Executing retry {} for document {}", 
            retryInfo.attempt(), documentId);

        try {
            processRetryOperation(retryInfo.operation());
            
            retryQueue.remove(documentId);
            cancelScheduledRetry(documentId);
            
            log.info("Retry successful for document {}", documentId);

        } catch (Exception e) {
            log.error("Retry {} failed for document {}", 
                retryInfo.attempt(), documentId, e);
            
            scheduleRetry(retryInfo.operation(), e);
        }
    }

    private void processRetryOperation(IndexOperation operation) {
        switch (operation.type()) {
            case CREATE, UPDATE, RESTORE -> {
                searchService.refreshIndex();
                log.info("Retry: Index refreshed for document {}", operation.documentId());
            }
            case DELETE -> {
                searchService.purgeDocumentFromIndex(operation.documentId());
                searchService.refreshIndex();
                log.info("Retry: Document {} purged from index", operation.documentId());
            }
        }
    }

    private void cancelScheduledRetry(Long documentId) {
        ScheduledFuture<?> future = scheduledRetries.remove(documentId);
        if (future != null) {
            future.cancel(false);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void checkStaleRetries() {
        retryQueue.forEach((documentId, retryInfo) -> {
            long age = System.currentTimeMillis() - retryInfo.timestamp();
            if (age > 5 * 60 * 1000) {
                log.warn("Stale retry detected for document {}, age={}ms", 
                    documentId, age);
                
                cancelScheduledRetry(documentId);
                retryQueue.remove(documentId);
            }
        });
    }

    @Scheduled(fixedRate = 30000)
    public void verifyIndexHealth() {
        try {
            if (!searchService.isIndexAvailable()) {
                log.warn("Index is not available, attempting to rebuild...");
            }
        } catch (Exception e) {
            log.error("Failed to verify index health", e);
        }
    }

    public int getRetryQueueSize() {
        return retryQueue.size();
    }

    public void clearRetryQueue() {
        scheduledRetries.values().forEach(f -> f.cancel(false));
        scheduledRetries.clear();
        retryQueue.clear();
        log.info("Retry queue cleared");
    }

    @PreDestroy
    public void shutdown() {
        clearRetryQueue();
    }

    public record RetryInfo(
        IndexOperation operation,
        int attempt,
        Exception lastException,
        long timestamp
    ) {
        public RetryInfo(IndexOperation operation, int attempt, Exception lastException) {
            this(operation, attempt, lastException, System.currentTimeMillis());
        }
    }
}

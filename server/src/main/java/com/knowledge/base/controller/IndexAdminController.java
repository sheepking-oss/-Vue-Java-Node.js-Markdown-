package com.knowledge.base.controller;

import com.knowledge.base.service.SearchService;
import com.knowledge.base.service.IndexRetryService;
import com.knowledge.base.service.IndexSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/index")
@RequiredArgsConstructor
public class IndexAdminController {

    private final SearchService searchService;
    private final IndexRetryService indexRetryService;
    private final IndexSyncService indexSyncService;

    @PostMapping("/rebuild")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> rebuildIndex() {
        log.info("Manual index rebuild requested");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            searchService.rebuildIndex();
            result.put("success", true);
            result.put("message", "Index rebuild completed successfully");
            log.info("Index rebuild completed successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Index rebuild failed", e);
            result.put("success", false);
            result.put("message", "Index rebuild failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> refreshIndex() {
        log.info("Manual index refresh requested");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            searchService.refreshIndex();
            result.put("success", true);
            result.put("message", "Index refreshed successfully");
            log.info("Index refreshed successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Index refresh failed", e);
            result.put("success", false);
            result.put("message", "Index refresh failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/optimize")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> optimizeIndex() {
        log.info("Manual index optimization requested");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            searchService.optimizeIndex();
            result.put("success", true);
            result.put("message", "Index optimized successfully");
            log.info("Index optimized successfully");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Index optimization failed", e);
            result.put("success", false);
            result.put("message", "Index optimization failed: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getIndexStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            boolean available = searchService.isIndexAvailable();
            long indexedCount = searchService.countIndexedDocuments();
            int pendingOps = indexSyncService.getPendingOperationCount();
            int retryQueueSize = indexRetryService.getRetryQueueSize();
            
            status.put("available", available);
            status.put("indexedDocumentCount", indexedCount);
            status.put("pendingOperations", pendingOps);
            status.put("retryQueueSize", retryQueueSize);
            status.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            log.error("Failed to get index status", e);
            status.put("available", false);
            status.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(status);
        }
    }

    @PostMapping("/clear-retries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearRetryQueue() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int before = indexRetryService.getRetryQueueSize();
            indexRetryService.clearRetryQueue();
            
            result.put("success", true);
            result.put("clearedItems", before);
            result.put("message", "Retry queue cleared");
            log.info("Retry queue cleared, removed {} items", before);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to clear retry queue", e);
            result.put("success", false);
            result.put("message", "Failed to clear retry queue: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    @PostMapping("/clear-pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> clearPendingOperations() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            int before = indexSyncService.getPendingOperationCount();
            indexSyncService.clearPendingOperations();
            
            result.put("success", true);
            result.put("clearedItems", before);
            result.put("message", "Pending operations cleared");
            log.info("Pending operations cleared, removed {} items", before);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to clear pending operations", e);
            result.put("success", false);
            result.put("message", "Failed to clear pending operations: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}

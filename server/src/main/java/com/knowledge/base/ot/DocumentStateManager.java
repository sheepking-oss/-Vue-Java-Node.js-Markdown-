package com.knowledge.base.ot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
public class DocumentStateManager {
    
    private final Map<Long, DocumentState> documentStates = new ConcurrentHashMap<>();
    
    private static class DocumentState {
        private final Lock lock = new ReentrantLock();
        private String currentContent;
        private int lastVersion;
        private final List<PendingOperation> pendingOps = new ArrayList<>();
        
        public DocumentState(String initialContent, int initialVersion) {
            this.currentContent = initialContent;
            this.lastVersion = initialVersion;
        }
    }
    
    public static class PendingOperation {
        private final List<Operation> operations;
        private final int fromVersion;
        private final String userId;
        private final long timestamp;
        
        public PendingOperation(List<Operation> operations, int fromVersion, String userId) {
            this.operations = operations;
            this.fromVersion = fromVersion;
            this.userId = userId;
            this.timestamp = System.currentTimeMillis();
        }
        
        public List<Operation> getOperations() {
            return operations;
        }
        
        public int getFromVersion() {
            return fromVersion;
        }
        
        public String getUserId() {
            return userId;
        }
    }
    
    public static class OperationResult {
        private final String newContent;
        private final List<Operation> transformedOps;
        private final int newVersion;
        private final boolean success;
        private final String errorMessage;
        
        public OperationResult(String newContent, List<Operation> transformedOps, int newVersion) {
            this.newContent = newContent;
            this.transformedOps = transformedOps;
            this.newVersion = newVersion;
            this.success = true;
            this.errorMessage = null;
        }
        
        public OperationResult(String errorMessage) {
            this.newContent = null;
            this.transformedOps = null;
            this.newVersion = -1;
            this.success = false;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getNewContent() {
            return newContent;
        }
        
        public List<Operation> getTransformedOps() {
            return transformedOps;
        }
        
        public int getNewVersion() {
            return newVersion;
        }
    }
    
    public void initializeDocument(Long documentId, String initialContent, int initialVersion) {
        documentStates.computeIfAbsent(documentId, 
            id -> new DocumentState(initialContent, initialVersion));
        log.info("Initialized document state for documentId: {}, version: {}", documentId, initialVersion);
    }
    
    public OperationResult applyOperations(
            Long documentId, 
            List<Operation> operations, 
            int fromVersion,
            String userId) {
        
        DocumentState state = documentStates.get(documentId);
        if (state == null) {
            return new OperationResult("Document state not initialized");
        }
        
        state.lock.lock();
        try {
            if (fromVersion > state.lastVersion) {
                return new OperationResult("Invalid version: " + fromVersion);
            }
            
            if (fromVersion < state.lastVersion) {
                log.info("Operations from older version: {} < {}, need to transform", fromVersion, state.lastVersion);
                
                List<Operation> concurrentOps = collectConcurrentOps(state, fromVersion);
                
                if (!concurrentOps.isEmpty()) {
                    log.info("Found {} concurrent operations to transform against", concurrentOps.size());
                    
                    List<Operation> currentOps = new ArrayList<>(operations);
                    
                    for (Operation concurrentOp : concurrentOps) {
                        List<Operation> opList = List.of(concurrentOp);
                        List<Operation> currentList = currentOps;
                        
                        OperationTransformer.OpPair transformed = OperationTransformer.transform(
                            currentList, opList);
                        
                        currentOps = transformed.first;
                    }
                    
                    operations = currentOps;
                    log.info("Transformed operations for userId: {}", userId);
                }
            }
            
            String newContent;
            try {
                newContent = OperationTransformer.apply(operations, state.currentContent);
            } catch (Exception e) {
                log.error("Failed to apply operations: {}", e.getMessage());
                return new OperationResult("Failed to apply operations: " + e.getMessage());
            }
            
            state.currentContent = newContent;
            state.lastVersion++;
            
            state.pendingOps.add(new PendingOperation(operations, fromVersion, userId));
            
            log.info("Applied operations for documentId: {}, version: {} -> {}, userId: {}", 
                documentId, state.lastVersion - 1, state.lastVersion, userId);
            
            return new OperationResult(newContent, operations, state.lastVersion);
            
        } finally {
            state.lock.unlock();
        }
    }
    
    private List<Operation> collectConcurrentOps(DocumentState state, int fromVersion) {
        List<Operation> result = new ArrayList<>();
        
        int opsToSkip = state.lastVersion - fromVersion;
        int startIdx = Math.max(0, state.pendingOps.size() - opsToSkip);
        
        for (int i = startIdx; i < state.pendingOps.size(); i++) {
            result.addAll(state.pendingOps.get(i).getOperations());
        }
        
        return result;
    }
    
    public String getCurrentContent(Long documentId) {
        DocumentState state = documentStates.get(documentId);
        return state != null ? state.currentContent : null;
    }
    
    public int getCurrentVersion(Long documentId) {
        DocumentState state = documentStates.get(documentId);
        return state != null ? state.lastVersion : -1;
    }
    
    public void updateDocumentState(Long documentId, String content, int version) {
        DocumentState state = documentStates.computeIfAbsent(documentId, 
            id -> new DocumentState(content, version));
        
        state.lock.lock();
        try {
            state.currentContent = content;
            state.lastVersion = version;
            log.info("Updated document state for documentId: {}, version: {}", documentId, version);
        } finally {
            state.lock.unlock();
        }
    }
    
    public void clearDocumentState(Long documentId) {
        documentStates.remove(documentId);
        log.info("Cleared document state for documentId: {}", documentId);
    }
    
    public boolean hasDocumentState(Long documentId) {
        return documentStates.containsKey(documentId);
    }
}

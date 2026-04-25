package com.knowledge.base.controller;

import com.knowledge.base.dto.OperationBatchDTO;
import com.knowledge.base.dto.OperationDTO;
import com.knowledge.base.dto.OperationResultDTO;
import com.knowledge.base.security.UserPrincipal;
import com.knowledge.base.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationController {

    private final DocumentService documentService;

    @PostMapping("/apply")
    public ResponseEntity<OperationResultDTO> applyOperations(
            @RequestBody OperationBatchDTO batchDTO,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        log.info("Applying {} operations for document {} from version {}", 
            batchDTO.getOperations().size(), 
            batchDTO.getDocumentId(), 
            batchDTO.getFromVersion());

        try {
            OperationResultDTO result = documentService.applyOperations(
                batchDTO.getDocumentId(),
                batchDTO.getOperations(),
                batchDTO.getFromVersion(),
                currentUser
            );

            if (result.isSuccess()) {
                log.info("Operations applied successfully, new version: {}", result.getNewVersion());
                return ResponseEntity.ok(result);
            } else {
                log.warn("Failed to apply operations: {}", result.getErrorMessage());
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            log.error("Error applying operations", e);
            OperationResultDTO errorResult = OperationResultDTO.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .build();
            return ResponseEntity.internalServerError().body(errorResult);
        }
    }

    @PostMapping("/diff")
    public ResponseEntity<List<OperationDTO>> computeDiff(
            @RequestBody Map<String, String> body) {
        
        String oldContent = body.getOrDefault("oldContent", "");
        String newContent = body.getOrDefault("newContent", "");

        log.info("Computing diff between content of length {} and {}", 
            oldContent.length(), newContent.length());

        try {
            List<OperationDTO> operations = documentService.diffContent(oldContent, newContent);
            log.info("Computed {} operations", operations.size());
            return ResponseEntity.ok(operations);
        } catch (Exception e) {
            log.error("Error computing diff", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/apply-to-content")
    public ResponseEntity<Map<String, String>> applyToContent(
            @RequestBody Map<String, Object> body) {
        
        try {
            String content = (String) body.get("content");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> operationMaps = (List<Map<String, Object>>) body.get("operations");
            
            List<OperationDTO> operations = operationMaps.stream()
                .map(map -> {
                    OperationDTO dto = new OperationDTO();
                    dto.setType((String) map.get("type"));
                    dto.setRetainCount((Integer) map.get("retainCount"));
                    dto.setInsertText((String) map.get("insertText"));
                    dto.setDeleteText((String) map.get("deleteText"));
                    return dto;
                })
                .toList();

            String result = documentService.applyOperationsToContent(content, operations);
            return ResponseEntity.ok(Map.of("result", result));
        } catch (Exception e) {
            log.error("Error applying operations to content", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/document/{documentId}/initialize")
    public ResponseEntity<?> initializeDocumentState(
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        String content = (String) body.getOrDefault("content", "");
        Integer version = (Integer) body.getOrDefault("version", 1);

        documentService.initializeDocumentState(documentId, content, version);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/document/{documentId}/state")
    public ResponseEntity<Map<String, Object>> getDocumentState(
            @PathVariable Long documentId) {
        
        Integer version = documentService.getDocumentStateVersion(documentId);
        String content = documentService.getDocumentStateContent(documentId);

        if (version == null || version < 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of(
            "documentId", documentId,
            "version", version,
            "content", content
        ));
    }

    @DeleteMapping("/document/{documentId}/state")
    public ResponseEntity<?> clearDocumentState(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        
        documentService.clearDocumentState(documentId);
        return ResponseEntity.ok().build();
    }
}

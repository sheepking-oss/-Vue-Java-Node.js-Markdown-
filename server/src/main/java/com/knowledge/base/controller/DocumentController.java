package com.knowledge.base.controller;

import com.knowledge.base.dto.DocumentDTO;
import com.knowledge.base.dto.UserDTO;
import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.DocumentVersion;
import com.knowledge.base.entity.Space;
import com.knowledge.base.security.UserPrincipal;
import com.knowledge.base.service.DocumentService;
import com.knowledge.base.service.SpaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final SpaceService spaceService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @PostMapping
    public ResponseEntity<?> createDocument(
            @RequestBody DocumentDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Document document = documentService.createDocument(request, currentUser);
        return ResponseEntity.ok(convertToDTO(document));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateDocument(
            @PathVariable Long id,
            @RequestBody DocumentDTO.UpdateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Document document = documentService.updateDocument(id, request, currentUser);
        return ResponseEntity.ok(convertToDTO(document));
    }

    @PostMapping("/{id}/auto-save")
    public ResponseEntity<?> autoSave(
            @PathVariable Long id,
            @RequestBody DocumentDTO.AutoSaveRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Document document = documentService.autoSave(id, request, currentUser);
        return ResponseEntity.ok(convertToDTO(document));
    }

    @PostMapping("/{id}/restore-draft")
    public ResponseEntity<?> restoreDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Document document = documentService.restoreDraft(id, currentUser);
        return ResponseEntity.ok(convertToDTO(document));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDocument(@PathVariable Long id) {
        return documentService.getDocument(id)
                .map(doc -> ResponseEntity.ok(convertToDTO(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/space/{spaceId}")
    public ResponseEntity<?> getSpaceDocuments(@PathVariable Long spaceId) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        List<Document> documents = documentService.getDocumentsBySpace(space);
        return ResponseEntity.ok(documents.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @GetMapping("/space/{spaceId}/tree")
    public ResponseEntity<?> getDocumentTree(@PathVariable Long spaceId) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        List<Document> rootDocs = documentService.getRootDocuments(space);
        return ResponseEntity.ok(rootDocs.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/trash")
    public ResponseEntity<?> moveToTrash(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        documentService.moveToTrash(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreFromTrash(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        documentService.restoreFromTrash(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePermanently(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        documentService.deletePermanently(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/space/{spaceId}/trash")
    public ResponseEntity<?> getTrashDocuments(@PathVariable Long spaceId) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        List<Document> documents = documentService.getTrashDocuments(space);
        return ResponseEntity.ok(documents.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<?> rollbackToVersion(
            @PathVariable Long id,
            @RequestParam Integer version,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Document document = documentService.rollbackToVersion(id, version, currentUser);
        return ResponseEntity.ok(convertToDTO(document));
    }

    @GetMapping("/{id}/versions")
    public ResponseEntity<?> getVersions(@PathVariable Long id) {
        Document document = documentService.getDocument(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        
        List<DocumentVersion> versions = documentService.getVersions(document);
        return ResponseEntity.ok(versions);
    }

    @GetMapping("/{id}/versions/{version}")
    public ResponseEntity<?> getVersion(@PathVariable Long id, @PathVariable Integer version) {
        Document document = documentService.getDocument(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        
        return documentService.getVersion(document, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam Long spaceId,
            @RequestParam String keyword) {
        Space space = spaceService.getSpace(spaceId)
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));
        
        List<Document> documents = documentService.searchDocuments(space, keyword);
        return ResponseEntity.ok(documents.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    @PostMapping("/{documentId}/tags/{tagId}")
    public ResponseEntity<?> addTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        documentService.addTag(documentId, tagId, currentUser);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{documentId}/tags/{tagId}")
    public ResponseEntity<?> removeTag(
            @PathVariable Long documentId,
            @PathVariable Long tagId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        documentService.removeTag(documentId, tagId, currentUser);
        return ResponseEntity.ok().build();
    }

    private DocumentDTO.Response convertToDTO(Document document) {
        DocumentDTO.Response dto = new DocumentDTO.Response();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setContent(document.getContent());
        dto.setDraftContent(document.getDraftContent());
        dto.setSpaceId(document.getSpace() != null ? document.getSpace().getId() : null);
        dto.setParentId(document.getParent() != null ? document.getParent().getId() : null);
        dto.setVersion(document.getVersion());
        dto.setDeleted(document.getIsDeleted());
        
        if (document.getCreatedBy() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(document.getCreatedBy().getId());
            userDTO.setUsername(document.getCreatedBy().getUsername());
            dto.setCreatedBy(userDTO);
        }
        
        if (document.getUpdatedBy() != null) {
            UserDTO userDTO = new UserDTO();
            userDTO.setId(document.getUpdatedBy().getId());
            userDTO.setUsername(document.getUpdatedBy().getUsername());
            dto.setUpdatedBy(userDTO);
        }
        
        dto.setCreatedAt(document.getCreatedAt() != null ? document.getCreatedAt().format(dateTimeFormatter) : null);
        dto.setUpdatedAt(document.getUpdatedAt() != null ? document.getUpdatedAt().format(dateTimeFormatter) : null);
        dto.setAutoSavedAt(document.getAutoSavedAt() != null ? document.getAutoSavedAt().format(dateTimeFormatter) : null);
        
        return dto;
    }
}

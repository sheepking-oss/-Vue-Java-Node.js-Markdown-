package com.knowledge.base.controller;

import com.knowledge.base.dto.ShareDTO;
import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.Share;
import com.knowledge.base.security.UserPrincipal;
import com.knowledge.base.service.DocumentService;
import com.knowledge.base.service.ShareService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shares")
@RequiredArgsConstructor
public class ShareController {

    private final ShareService shareService;
    private final DocumentService documentService;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @PostMapping
    public ResponseEntity<?> createShare(
            @RequestBody ShareDTO.CreateRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Share.ShareType type = Share.ShareType.valueOf(request.getType().toUpperCase());
        Share share = shareService.createShare(
                request.getDocumentId(),
                type,
                request.getPassword(),
                request.getExpiresInHours(),
                currentUser
        );
        return ResponseEntity.ok(convertToDTO(share));
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<?> toggleShare(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Share share = shareService.toggleShare(id, currentUser);
        return ResponseEntity.ok(convertToDTO(share));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteShare(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        shareService.deleteShare(id, currentUser);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/access")
    public ResponseEntity<?> accessShare(@RequestBody ShareDTO.AccessRequest request) {
        try {
            Document document = shareService.accessShare(request.getShareCode(), request.getPassword())
                    .orElseThrow(() -> new IllegalArgumentException("Share not found"));
            
            return ResponseEntity.ok(document);
        } catch (SecurityException e) {
            return ResponseEntity.status(401).body("Invalid password");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/document/{documentId}")
    public ResponseEntity<?> getDocumentShares(
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserPrincipal currentUser) {
        Document document = documentService.getDocument(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));
        
        List<Share> shares = shareService.getDocumentShares(document);
        return ResponseEntity.ok(shares.stream().map(this::convertToDTO).collect(Collectors.toList()));
    }

    private ShareDTO.Response convertToDTO(Share share) {
        ShareDTO.Response dto = new ShareDTO.Response();
        dto.setId(share.getId());
        dto.setDocumentId(share.getDocument() != null ? share.getDocument().getId() : null);
        dto.setShareCode(share.getShareCode());
        dto.setType(share.getType().name().toLowerCase());
        dto.setEnabled(share.getIsEnabled());
        dto.setHasPassword(share.getPassword() != null);
        dto.setExpiresAt(share.getExpiresAt() != null ? share.getExpiresAt().format(dateTimeFormatter) : null);
        dto.setCreatedAt(share.getCreatedAt() != null ? share.getCreatedAt().format(dateTimeFormatter) : null);
        return dto;
    }
}

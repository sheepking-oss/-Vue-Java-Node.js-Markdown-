package com.knowledge.base.service;

import com.knowledge.base.entity.Document;
import com.knowledge.base.entity.Share;
import com.knowledge.base.repository.DocumentRepository;
import com.knowledge.base.repository.ShareRepository;
import com.knowledge.base.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShareService {

    private final ShareRepository shareRepository;
    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Share createShare(Long documentId, Share.ShareType type, String password, Integer expiresInHours, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!documentService.hasViewPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to share this document");
        }

        Share share = new Share();
        share.setDocument(document);
        share.setShareCode(generateShareCode());
        share.setType(type);
        share.setCreatedBy(currentUser.getUser());

        if (password != null && !password.isEmpty()) {
            share.setPassword(passwordEncoder.encode(password));
        }

        if (expiresInHours != null && expiresInHours > 0) {
            share.setExpiresAt(LocalDateTime.now().plusHours(expiresInHours));
        }

        return shareRepository.save(share);
    }

    @Transactional
    public Share toggleShare(Long shareId, UserPrincipal currentUser) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        if (!share.getCreatedBy().getId().equals(currentUser.getUser().getId())) {
            throw new SecurityException("No permission to manage this share");
        }

        share.setIsEnabled(!share.getIsEnabled());
        return shareRepository.save(share);
    }

    @Transactional
    public void deleteShare(Long shareId, UserPrincipal currentUser) {
        Share share = shareRepository.findById(shareId)
                .orElseThrow(() -> new IllegalArgumentException("Share not found"));

        if (!share.getCreatedBy().getId().equals(currentUser.getUser().getId())) {
            throw new SecurityException("No permission to delete this share");
        }

        shareRepository.delete(share);
    }

    public Optional<Document> accessShare(String shareCode, String password) {
        Share share = shareRepository.findByShareCodeAndIsEnabledTrue(shareCode)
                .orElseThrow(() -> new IllegalArgumentException("Share not found or disabled"));

        if (share.getExpiresAt() != null && LocalDateTime.now().isAfter(share.getExpiresAt())) {
            throw new IllegalArgumentException("Share link has expired");
        }

        if (share.getPassword() != null) {
            if (password == null || !passwordEncoder.matches(password, share.getPassword())) {
                throw new SecurityException("Invalid password");
            }
        }

        return Optional.of(share.getDocument());
    }

    public List<Share> getDocumentShares(Document document) {
        return shareRepository.findByDocument(document);
    }

    public Optional<Share> getShare(Long shareId) {
        return shareRepository.findById(shareId);
    }

    private String generateShareCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        } while (shareRepository.findByShareCode(code).isPresent());
        return code;
    }
}

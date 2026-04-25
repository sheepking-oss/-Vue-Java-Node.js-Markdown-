package com.knowledge.base.service;

import com.knowledge.base.dto.DocumentDTO;
import com.knowledge.base.entity.*;
import com.knowledge.base.repository.*;
import com.knowledge.base.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository versionRepository;
    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final TagRepository tagRepository;

    @Transactional
    public Document createDocument(DocumentDTO.CreateRequest request, UserPrincipal currentUser) {
        Space space = spaceRepository.findById(request.getSpaceId())
                .orElseThrow(() -> new IllegalArgumentException("Space not found"));

        if (!hasEditPermission(space, currentUser.getUser())) {
            throw new SecurityException("No permission to create document");
        }

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent() != null ? request.getContent() : "");
        document.setSpace(space);
        document.setCreatedBy(currentUser.getUser());
        document.setUpdatedBy(currentUser.getUser());

        if (request.getParentId() != null) {
            Document parent = documentRepository.findByIdAndIsDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent document not found"));
            document.setParent(parent);
        }

        document = documentRepository.save(document);

        DocumentVersion version = createVersion(document, currentUser.getUser(), "Initial version");
        document.setCurrentVersion(version);
        document.setVersion(version.getVersion());

        return documentRepository.save(document);
    }

    @Transactional
    public Document updateDocument(Long documentId, DocumentDTO.UpdateRequest request, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to update document");
        }

        if (request.getTitle() != null) {
            document.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            document.setContent(request.getContent());
        }
        if (request.getParentId() != null) {
            Document parent = documentRepository.findByIdAndIsDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent document not found"));
            document.setParent(parent);
        }

        document.setUpdatedBy(currentUser.getUser());

        String changeNote = request.getChangeNote() != null ? request.getChangeNote() : "Updated content";
        DocumentVersion version = createVersion(document, currentUser.getUser(), changeNote);
        document.setCurrentVersion(version);
        document.setVersion(version.getVersion());

        return documentRepository.save(document);
    }

    @Transactional
    public Document autoSave(Long documentId, DocumentDTO.AutoSaveRequest request, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to edit document");
        }

        document.setDraftContent(request.getDraftContent());
        document.setAutoSavedAt(LocalDateTime.now());
        document.setUpdatedBy(currentUser.getUser());

        return documentRepository.save(document);
    }

    @Transactional
    public Document restoreDraft(Long documentId, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to edit document");
        }

        if (document.getDraftContent() != null) {
            document.setContent(document.getDraftContent());
            document.setDraftContent(null);
            document.setUpdatedBy(currentUser.getUser());

            DocumentVersion version = createVersion(document, currentUser.getUser(), "Restored from draft");
            document.setCurrentVersion(version);
            document.setVersion(version.getVersion());
        }

        return documentRepository.save(document);
    }

    @Transactional
    public void moveToTrash(Long documentId, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to delete document");
        }

        document.setIsDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setDeletedBy(currentUser.getUser());

        documentRepository.save(document);
    }

    @Transactional
    public void restoreFromTrash(Long documentId, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedTrue(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found in trash"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to restore document");
        }

        document.setIsDeleted(false);
        document.setDeletedAt(null);
        document.setDeletedBy(null);

        documentRepository.save(document);
    }

    @Transactional
    public void deletePermanently(Long documentId, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedTrue(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found in trash"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to delete document");
        }

        documentRepository.delete(document);
    }

    @Transactional
    public Document rollbackToVersion(Long documentId, Integer targetVersion, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to rollback document");
        }

        DocumentVersion target = versionRepository.findByDocumentAndVersion(document, targetVersion)
                .orElseThrow(() -> new IllegalArgumentException("Version not found"));

        document.setContent(target.getContent());
        document.setTitle(target.getTitle());
        document.setUpdatedBy(currentUser.getUser());

        String changeNote = String.format("Rollback to version %d", targetVersion);
        DocumentVersion newVersion = createVersion(document, currentUser.getUser(), changeNote);
        document.setCurrentVersion(newVersion);
        document.setVersion(newVersion.getVersion());

        return documentRepository.save(document);
    }

    @Transactional
    public void addTag(Long documentId, Long tagId, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to modify document");
        }

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        document.getTags().add(tag);
        documentRepository.save(document);
    }

    @Transactional
    public void removeTag(Long documentId, Long tagId, UserPrincipal currentUser) {
        Document document = documentRepository.findByIdAndIsDeletedFalse(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        if (!hasEditPermission(document.getSpace(), currentUser.getUser())) {
            throw new SecurityException("No permission to modify document");
        }

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new IllegalArgumentException("Tag not found"));

        document.getTags().remove(tag);
        documentRepository.save(document);
    }

    public Optional<Document> getDocument(Long documentId) {
        return documentRepository.findByIdAndIsDeletedFalse(documentId);
    }

    public List<Document> getDocumentsBySpace(Space space) {
        return documentRepository.findBySpaceAndIsDeletedFalseOrderByUpdatedAtDesc(space);
    }

    public List<Document> getRootDocuments(Space space) {
        return documentRepository.findBySpaceAndParentIsNullAndIsDeletedFalseOrderByCreatedAtAsc(space);
    }

    public List<Document> getTrashDocuments(Space space) {
        return documentRepository.findBySpaceAndIsDeletedTrueOrderByDeletedAtDesc(space);
    }

    public List<Document> searchDocuments(Space space, String keyword) {
        return documentRepository.searchByKeyword(space, keyword);
    }

    public List<DocumentVersion> getVersions(Document document) {
        return versionRepository.findByDocumentOrderByVersionDesc(document);
    }

    public Optional<DocumentVersion> getVersion(Document document, Integer version) {
        return versionRepository.findByDocumentAndVersion(document, version);
    }

    private DocumentVersion createVersion(Document document, User user, String changeNote) {
        Integer nextVersion = versionRepository.countByDocument(document) + 1;

        DocumentVersion version = new DocumentVersion();
        version.setDocument(document);
        version.setVersion(nextVersion);
        version.setTitle(document.getTitle());
        version.setContent(document.getContent());
        version.setCreatedBy(user);
        version.setChangeNote(changeNote);

        return versionRepository.save(version);
    }

    private boolean hasEditPermission(Space space, User user) {
        if (space.getOwner().getId().equals(user.getId())) {
            return true;
        }

        Optional<SpaceMember> member = spaceMemberRepository.findBySpaceAndUser(space, user);
        if (member.isEmpty()) {
            return false;
        }

        SpaceMember.Role role = member.get().getRole();
        return role == SpaceMember.Role.ADMIN || role == SpaceMember.Role.EDITOR;
    }

    public boolean hasViewPermission(Space space, User user) {
        if (space.getOwner().getId().equals(user.getId())) {
            return true;
        }
        return spaceMemberRepository.existsBySpaceAndUser(space, user);
    }
}

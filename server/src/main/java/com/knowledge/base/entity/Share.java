package com.knowledge.base.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "shares")
public class Share {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(unique = true, nullable = false)
    private String shareCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShareType type = ShareType.VIEW;

    @Column(nullable = false)
    private Boolean isEnabled = true;

    private LocalDateTime expiresAt;

    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "share", fetch = FetchType.LAZY)
    private Set<ShareAccess> accessLogs = new HashSet<>();

    public enum ShareType {
        VIEW,
        EDIT
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

package com.knowledge.base.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "space_members")
public class SpaceMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "space_id", nullable = false)
    private Space space;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.VIEWER;

    public enum Role {
        OWNER,
        ADMIN,
        EDITOR,
        VIEWER
    }
}

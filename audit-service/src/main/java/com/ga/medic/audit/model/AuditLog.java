package com.ga.medic.audit.model;

import com.ga.medic.audit.enums.AuditAction;
import com.ga.medic.audit.enums.AuditEntityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    private AuditEntityType entityType;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public AuditLog(String username, AuditAction action, AuditEntityType entityType, String description) {
        this.username = username;
        this.action = action;
        this.entityType = entityType;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }
}

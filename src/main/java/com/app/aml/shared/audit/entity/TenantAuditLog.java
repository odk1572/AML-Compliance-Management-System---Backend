package com.app.aml.shared.audit.entity;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantAuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "action_category", nullable = false, length = 50)
    private String actionCategory;

    @Column(name = "action_performed", nullable = false, length = 255)
    private String actionPerformed;

    @Column(name = "target_entity_type", nullable = false, length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prev_state", columnDefinition = "jsonb")
    private String prevState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "next_state", columnDefinition = "jsonb")
    private String nextState;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt;
}
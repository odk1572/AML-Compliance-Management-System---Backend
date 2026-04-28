
package com.app.aml.audit.entity;


import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "platform_audit_log", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformAuditLog {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    @Builder.Default
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_role", nullable = false, length = 50)
    private String actorRole;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "schema_name", length = 63)
    private String schemaName;

    @Column(name = "action_category", nullable = false, length = 50)
    private String actionCategory;

    @Column(name = "action_performed", nullable = false, length = 255)
    private String actionPerformed;

    @Column(name = "target_entity_type", nullable = false, length = 100)
    private String targetEntityType;

    @Column(name = "target_entity_id")
    private UUID targetEntityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    private String previousState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", columnDefinition = "jsonb")
    private String newState;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @CreationTimestamp
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt;
}
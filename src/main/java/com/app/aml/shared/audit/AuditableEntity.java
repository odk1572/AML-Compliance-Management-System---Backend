package com.app.aml.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor // Required by JPA
@AllArgsConstructor // Required by SuperBuilder
@SuperBuilder // Required for hierarchy building
public abstract class AuditableEntity {

    @CreationTimestamp
    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt;

    @UpdateTimestamp
    @Column(name = "sys_updated_at")
    private Instant sysUpdatedAt;
}
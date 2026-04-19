package com.app.aml.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@MappedSuperclass
public abstract class AuditableEntity {

    @Column(name = "sys_created_at", nullable = false, updatable = false)
    private Instant sysCreatedAt;

    @Column(name = "sys_updated_at", nullable = false)
    private Instant sysUpdatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.sysCreatedAt = now;
        this.sysUpdatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.sysUpdatedAt = Instant.now();
    }
}
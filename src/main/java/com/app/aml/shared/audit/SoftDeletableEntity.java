package com.app.aml.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
@Getter
@Setter
@MappedSuperclass
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Column(name = "sys_is_deleted", nullable = false)
    private boolean sysIsDeleted = false;

    @Column(name = "sys_deleted_at")
    private Instant sysDeletedAt;

    public void markDeleted() {
        this.sysIsDeleted = true;
        this.sysDeletedAt = Instant.now();
    }
}
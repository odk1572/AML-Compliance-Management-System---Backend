package com.app.aml.audit;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
@Getter
@Setter
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
public abstract class SoftDeletableEntity extends AuditableEntity {

    @Builder.Default
    @Column(name = "sys_is_deleted", nullable = false)
    private boolean sysIsDeleted = false;

    @Column(name = "sys_deleted_at")
    private Instant sysDeletedAt;

}
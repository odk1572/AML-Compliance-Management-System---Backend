package com.app.aml.feature.tenant.entity;

import com.app.aml.enums.TenantStatus;
import com.app.aml.shared.audit.SoftDeletableEntity;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "common_schema")
@Getter
@Setter
@NoArgsConstructor
public class Tenant extends SoftDeletableEntity {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id = UuidCreator.getTimeOrderedEpoch();

    @NotBlank
    @Size(max = 50)
    @Column(name = "tenant_code", unique = true, nullable = false, length = 50)
    private String tenantCode;

    @NotBlank
    @Size(max = 63)
    @Column(name = "schema_name", unique = true, nullable = false, length = 63)
    private String schemaName;

    @NotBlank
    @Size(max = 255)
    @Column(name = "institution_name", nullable = false, length = 255)
    private String institutionName;

    @NotBlank
    @Size(max = 3)
    @Column(name = "country_code", nullable = false, length = 3)
    private String countryCode;

    @Size(max = 100)
    @Column(name = "regulatory_jurisdiction", length = 100)
    private String regulatoryJurisdiction;

    @Email
    @Size(max = 255)
    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Size(max = 50)
    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "sys_deleted_by")
    private UUID sysDeletedBy;

    @Column(name = "sys_created_by")
    private UUID sysCreatedBy;
}
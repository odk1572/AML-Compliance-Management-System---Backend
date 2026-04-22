package com.app.aml.feature.tenant.dto.response;

import com.app.aml.domain.enums.TenantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantResponseDto {
    private UUID id;
    private String tenantCode;
    private String schemaName;
    private String institutionName;
    private String countryCode;
    private String regulatoryJurisdiction;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private TenantStatus status;
    private Instant sysCreatedAt;
    private Instant sysUpdatedAt;
}
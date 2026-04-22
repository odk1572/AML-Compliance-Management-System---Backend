package com.app.aml.feature.tenant.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequestDto {

    @NotBlank(message = "Tenant code is required")
    @Size(max = 50, message = "Tenant code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Tenant code must be alphanumeric and uppercase")
    private String tenantCode;

    @NotBlank(message = "Schema name is required")
    @Size(max = 63, message = "Schema name cannot exceed 63 characters")
    @Pattern(regexp = "^[a-z0-9_]+$", message = "Schema name must be lowercase alphanumeric with underscores")
    private String schemaName;

    @NotBlank(message = "Institution name is required")
    @Size(max = 255)
    private String institutionName;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 3, message = "Country code must be 2 or 3 characters")
    private String countryCode;

    @Size(max = 100)
    private String regulatoryJurisdiction;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50)
    private String contactPhone;

    private String address;
}
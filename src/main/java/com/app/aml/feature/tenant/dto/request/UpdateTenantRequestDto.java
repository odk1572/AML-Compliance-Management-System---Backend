package com.app.aml.feature.tenant.dto.request;

import com.app.aml.domain.enums.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequestDto {

    @NotBlank(message = "Institution name is required")
    @Size(max = 255)
    private String institutionName;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 3)
    private String countryCode;

    @Size(max = 100)
    private String regulatoryJurisdiction;

    @Email(message = "Invalid email format")
    @Size(max = 255)
    private String contactEmail;

    @Size(max = 50)
    private String contactPhone;

    private String address;

    @NotNull(message = "Tenant status is required")
    private TenantStatus status;
}
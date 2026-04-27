package com.app.aml.feature.ingestion.dto.customerProfile.response;
import com.app.aml.enums.CustomerType;
import com.app.aml.enums.KycStatus; // Assuming this exists
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.alert.dto.alert.response.AlertResponseDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerProfileResponseDto {
    private UUID id;
    private String accountNumber;
    private String customerName;
    private CustomerType customerType;
    private String nationality;
    private String countryOfResidence;
    private String riskRating;
    private Integer riskScore;
    private boolean isPep;
    private boolean isDormant;
    private LocalDate accountOpenedOn;
    private LocalDate lastActivityDate;
    private KycStatus kycStatus;

    private String kycDocumentUrl;

    private List<AlertResponseDto> recentAlerts;
    private List<CaseResponseDto> recentCases;
}
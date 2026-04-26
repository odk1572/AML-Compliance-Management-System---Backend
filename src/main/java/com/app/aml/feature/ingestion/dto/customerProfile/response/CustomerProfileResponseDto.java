package com.app.aml.feature.ingestion.dto.customerProfile.response;
import com.app.aml.domain.enums.CustomerType;
import com.app.aml.domain.enums.KycStatus; // Assuming this exists
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.ingestion.dto.alert.response.AlertResponseDto;
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

    // Media
    private String kycDocumentUrl;

    // 360 View Aggregations
    private List<AlertResponseDto> recentAlerts;
    private List<CaseResponseDto> recentCases;
}
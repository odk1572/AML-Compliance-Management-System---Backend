package com.app.aml.feature.alert.dto.alert;

import com.app.aml.feature.alert.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.alert.dto.alertEvidence.response.AlertEvidenceResponseDto;
import com.app.aml.feature.alert.dto.alertTransaction.AlertTransactionSummaryDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertDetailResponseDto {
    private AlertResponseDto alert;
    private List<AlertEvidenceResponseDto> evidences;
    private List<AlertTransactionSummaryDto> linkedTransactions;
}
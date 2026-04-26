package com.app.aml.feature.ingestion.dto.alert;

import com.app.aml.feature.ingestion.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.ingestion.dto.alertEvidence.response.AlertEvidenceResponseDto;
import com.app.aml.feature.ingestion.dto.alertTransaction.AlertTransactionSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDetailResponseDto {
    private AlertResponseDto alert;
    private List<AlertEvidenceResponseDto> evidences;
    private List<AlertTransactionSummaryDto> linkedTransactions;
}
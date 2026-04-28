package com.app.aml.feature.reporting.service;

import com.app.aml.enums.CaseStatus;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.reporting.dtos.TenantReportDtos.*;
import com.app.aml.feature.strfiling.repository.StrFilingRepository;
import com.app.aml.annotation.AuditAction;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('BANK_ADMIN')")
public class TenantReportServiceImpl implements TenantReportService {

    private final AlertRepository alertRepo;
    private final StrFilingRepository strRepo;
    private final TransactionBatchRepository batchRepo;
    private final CaseRecordRepository caseRepo;


    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "REPORTING", action = "VIEW_STR_LOG", entityType = "STR")
    public Page<StrLogDto> getStrLog(Pageable pageable) {
        return strRepo.findAll(pageable).map(filing -> StrLogDto.builder()
                .filingReference(filing.getFilingReference())
                .typology(filing.getRuleType())
                .subjectName(filing.getCustomer().getCustomerName())
                .filedBy(filing.getFiledBy())
                .filedAt(filing.getSysCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "REPORTING", action = "VIEW_BATCH_REPORT", entityType = "BATCH")
    public Page<BatchSummaryDto> getBatchSummary(Pageable pageable) {
        return batchRepo.findAll(pageable).map(batch -> BatchSummaryDto.builder()
                .batchId(batch.getId())
                .fileName(batch.getFileName())
                .status(batch.getBatchStatus().name())
                .totalRecords(batch.getTotalRecords())
                .uploadedAt(batch.getSysCreatedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "REPORTING", action = "VIEW_CO_PERFORMANCE", entityType = "ANALYTICS")
    public List<CoPerformanceDto> getCoPerformance() {
        // Get all closed cases to calculate lead time
        List<CaseRecord> closedCases = caseRepo.findByStatusIn(List.of(CaseStatus.CLOSED_NO_ACTION,CaseStatus.CLOSED_STR));

        // Group by Compliance Officer (assigned_to)
        Map<Object, List<CaseRecord>> groupedByCo = closedCases.stream()
                .filter(c -> c.getAssignedTo() != null && c.getClosedAt() != null)
                .collect(Collectors.groupingBy(CaseRecord::getAssignedTo));

        return groupedByCo.entrySet().stream()
                .map(entry -> {
                    List<CaseRecord> cases = entry.getValue();
                    double avgDays = cases.stream()
                            .mapToLong(c -> Duration.between(c.getOpenedAt(), c.getClosedAt()).toDays())
                            .average()
                            .orElse(0.0);

                    return CoPerformanceDto.builder()
                            .coId(cases.get(0).getAssignedTo())
                            .coName("CO-" + cases.get(0).getAssignedTo().toString().substring(0, 5))
                            .avgDaysToClose(avgDays)
                            .totalClosed((long) cases.size())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
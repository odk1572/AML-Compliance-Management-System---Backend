package com.app.aml.feature.ingestion.service;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.domain.enums.AlertStatus;
import com.app.aml.feature.ingestion.dto.alert.AlertDetailResponseDto;
import com.app.aml.feature.ingestion.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.ingestion.dto.alertEvidence.response.AlertEvidenceResponseDto;
import com.app.aml.feature.ingestion.dto.alertTransaction.AlertTransactionSummaryDto;
import com.app.aml.feature.ingestion.entity.Alert;
import com.app.aml.feature.ingestion.mapper.AlertEvidenceMapper;
import com.app.aml.feature.ingestion.mapper.AlertMapper;
import com.app.aml.feature.ingestion.repository.AlertEvidenceRepository;
import com.app.aml.feature.ingestion.repository.AlertRepository;
import com.app.aml.feature.ingestion.repository.AlertTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertDashboardServiceImpl implements AlertDashboardService {

    private final AlertRepository alertRepo;
    private final AlertEvidenceRepository evidenceRepo;
    private final AlertTransactionRepository alertTxnRepo;

    private final AlertMapper alertMapper;
    private final AlertEvidenceMapper evidenceMapper;
    @Override
    @Transactional(readOnly = true)
    public Page<AlertResponseDto> getAlerts(AlertSeverity severity, AlertStatus status,
                                            LocalDate from, LocalDate to, Pageable pageable) {

        // Convert LocalDate -> ZonedDateTime -> Instant
        Instant start = (from != null)
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.parse("2000-01-01T00:00:00Z");

        Instant end = (to != null)
                ? to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
                : Instant.parse("2099-12-31T23:59:59.999Z");

        // Pass the perfectly formatted Instants to your repository
        return alertRepo.findWithFilters(severity, status, start, end, pageable)
                .map(alert -> alertMapper.toResponseDto(alert));
    }
    @Override
    @Transactional(readOnly = true)
    public AlertDetailResponseDto getAlertDetail(UUID alertId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found with ID: " + alertId));

        List<AlertEvidenceResponseDto> evidences = evidenceRepo.findByAlertId(alertId).stream()
                .map(evidenceMapper::toResponseDto)
                .toList();

        List<AlertTransactionSummaryDto> linkedTransactions = alertTxnRepo.findByAlertId(alertId).stream()
                .map(at -> new AlertTransactionSummaryDto(
                        at.getTransaction().getId(),
                        at.getTransaction().getTransactionRef(),
                        at.getTransaction().getAmount(),
                        at.getTransaction().getCurrencyCode(),
                        at.getInvolvementRole()
                ))
                .toList();

        return AlertDetailResponseDto.builder()
                .alert(alertMapper.toResponseDto(alert))
                .evidences(evidences)
                .linkedTransactions(linkedTransactions)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getSeverityCounts() {
        List<Object[]> results = alertRepo.countByStatusAndGroupBySeverity(AlertStatus.NEW);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
    }
    @Transactional
    public void closeAlert(UUID alertId, AlertStatus resolution, String comment) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert alertId " + alertId + " not found"));

        // 1. Validation: Ensure we are only using "Final" statuses here
        if (resolution != AlertStatus.CLOSED_FALSE_POSITIVE && resolution != AlertStatus.CLOSED_CONFIRMED) {
            throw new IllegalArgumentException("Use bundleToCase() for investigations. This method is only for final closure.");
        }

        // 3. Update Status
        alert.setStatus(resolution);

        alertRepo.save(alert);
    }
}
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

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        var start = (from != null) ? from.atStartOfDay() : LocalDateTime.of(2000, 1, 1, 0, 0);
        var end = (to != null) ? to.atTime(LocalTime.MAX) : LocalDateTime.of(2099, 12, 31, 23, 59);

        // Use a lambda instead of alertMapper::toResponseDto
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
    public void assignAlert(UUID alertId, UUID userId) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        alert.setSysUpdatedAt(Instant.now());
        alert.setStatus(AlertStatus.NEW);
        alertRepo.save(alert);
    }

    @Transactional
    public void closeAlert(UUID alertId, AlertStatus resolution, String comment) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert not found"));

        if (resolution != AlertStatus.CLOSED_FALSE_POSITIVE && resolution != AlertStatus.CLOSED_CONFIRMED) {
            throw new IllegalArgumentException("Invalid closing status for investigation finalization");
        }

        alert.setStatus(resolution);
        alertRepo.save(alert);
    }
}
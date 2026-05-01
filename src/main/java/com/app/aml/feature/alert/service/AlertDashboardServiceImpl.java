package com.app.aml.feature.alert.service;

import com.app.aml.enums.AlertSeverity;
import com.app.aml.enums.AlertStatus;
import com.app.aml.feature.alert.dto.alert.AlertDetailResponseDto;
import com.app.aml.feature.alert.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.alert.dto.alertEvidence.response.AlertEvidenceResponseDto;
import com.app.aml.feature.alert.dto.alertTransaction.AlertTransactionSummaryDto;
import com.app.aml.feature.alert.entity.Alert;
import com.app.aml.feature.alert.mapper.AlertEvidenceMapper;
import com.app.aml.feature.alert.mapper.AlertMapper;
import com.app.aml.feature.alert.repository.AlertEvidenceRepository;
import com.app.aml.feature.alert.repository.AlertRepository;
import com.app.aml.feature.alert.repository.AlertTransactionRepository;
import com.app.aml.annotation.AuditAction;

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
    @AuditAction(category = "DATA_ACCESS", action = "LIST_ALERTS", entityType = "ALERT")
    public Page<AlertResponseDto> getAlerts(AlertSeverity severity, AlertStatus status,
                                            LocalDate from, LocalDate to, Pageable pageable, String customer) {

        Instant start = (from != null)
                ? from.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : Instant.parse("2000-01-01T00:00:00Z");

        Instant end = (to != null)
                ? to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant()
                : Instant.parse("2099-12-31T23:59:59.999Z");

        String customerPattern = (customer != null && !customer.trim().isEmpty())
                ? "%" + customer.toLowerCase() + "%"
                : null;

        return alertRepo.findWithFilters(severity, status, start, end, pageable, customerPattern)
                .map(alertMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_ALERT_DETAILS", entityType = "ALERT")
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
                        at.getTransaction().getTransactionTimestamp(),
                        at.getTransaction().getTransactionType() != null ?
                                at.getTransaction().getTransactionType().name() : null,
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
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_DASHBOARD_STATS", entityType = "ALERT")
    public Map<String, Long> getSeverityCounts() {
        List<Object[]> results = alertRepo.countByStatusAndGroupBySeverity(AlertStatus.NEW);
        return results.stream()
                .collect(Collectors.toMap(
                        row -> row[0].toString(),
                        row -> (Long) row[1]
                ));
    }

    @Transactional
    @AuditAction(category = "ALERT_MGMT", action = "CLOSE_ALERT", entityType = "ALERT")
    public void closeAlert(UUID alertId, AlertStatus resolution, String comment) {
        Alert alert = alertRepo.findById(alertId)
                .orElseThrow(() -> new EntityNotFoundException("Alert alertId " + alertId + " not found"));

        if (resolution != AlertStatus.CLOSED_FALSE_POSITIVE && resolution != AlertStatus.CLOSED_CONFIRMED && resolution != AlertStatus.UNDER_REVIEW) {
            throw new IllegalArgumentException("Use bundleToCase() for investigations. This method is only for final closure or confirmation.");
        }

        alert.setStatus(resolution);
        alertRepo.save(alert);
    }
}
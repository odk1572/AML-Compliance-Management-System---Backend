package com.app.aml.feature.ingestion.service;

import com.app.aml.domain.enums.AlertSeverity;
import com.app.aml.domain.enums.AlertStatus;
import com.app.aml.feature.ingestion.dto.alert.AlertDetailResponseDto;
import com.app.aml.feature.ingestion.dto.alert.response.AlertResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public interface AlertDashboardService {
    Page<AlertResponseDto> getAlerts(AlertSeverity severity, AlertStatus status,
                                     LocalDate from, LocalDate to, Pageable pageable);
    AlertDetailResponseDto getAlertDetail(UUID alertId);
    Map<String, Long> getSeverityCounts();
    void closeAlert(UUID alertId, AlertStatus resolution, String comment);

}

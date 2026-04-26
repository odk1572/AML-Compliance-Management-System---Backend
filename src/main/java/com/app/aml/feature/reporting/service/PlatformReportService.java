package com.app.aml.feature.reporting.service;


import com.app.aml.feature.reporting.dtos.PlatformReportDtos.*;
import java.time.LocalDate;
import java.util.List;

public interface PlatformReportService {
    List<SarSummaryDto> getSarSummary(LocalDate from, LocalDate to);
}
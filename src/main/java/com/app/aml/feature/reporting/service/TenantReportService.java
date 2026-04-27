package com.app.aml.feature.reporting.service;


import com.app.aml.feature.reporting.dtos.TenantReportDtos.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TenantReportService {
    Page<StrLogDto> getStrLog(Pageable pageable);
    Page<BatchSummaryDto> getBatchSummary(Pageable pageable);
    List<CoPerformanceDto> getCoPerformance();
}
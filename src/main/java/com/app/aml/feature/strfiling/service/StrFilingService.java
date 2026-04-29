package com.app.aml.feature.strfiling.service;

import com.app.aml.feature.strfiling.dto.strFiling.StrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;

import java.util.List;
import java.util.UUID;

public interface StrFilingService {
    StrFilingResponseDto fileSar(UUID caseId, StrFilingRequestDto dto, UUID filedBy, String ip);
    StrFilingResponseDto getFilingDetail(UUID filingId);
    void validateGate(UUID caseId);
    byte[] getPdfReport(UUID filingId);
    StrFilingResponseDto getFilingByCaseId(UUID caseId);
    List<StrFilingResponseDto> getAllFilings();
}

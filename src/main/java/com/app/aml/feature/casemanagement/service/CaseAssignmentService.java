package com.app.aml.feature.casemanagement.service;

import com.app.aml.feature.casemanagement.dto.CreateCaseRequest;
import com.app.aml.feature.casemanagement.dto.ReassignCaseRequest;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CaseAssignmentService {

    CaseResponseDto createCase(CreateCaseRequest request);
    CaseResponseDto getCaseDetails(String caseReference);
    void reassignCase(String caseReference, ReassignCaseRequest request);
    Page<CaseResponseDto> getAllCases(Pageable pageable);
}

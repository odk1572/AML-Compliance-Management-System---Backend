package com.app.aml.feature.casemanagement.service;

import com.app.aml.feature.casemanagement.dto.CreateCaseRequest;
import com.app.aml.feature.casemanagement.dto.ReassignCaseRequest;
import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;

import java.util.List;
import java.util.UUID;

public interface CaseAssignmentService {

    CaseResponseDto createCase(CreateCaseRequest request);
    CaseResponseDto getCaseDetails(String caseReference);
    void reassignCase(String caseReference, ReassignCaseRequest request);
}

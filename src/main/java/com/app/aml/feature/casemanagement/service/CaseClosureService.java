package com.app.aml.feature.casemanagement.service;


import java.util.UUID;

public interface CaseClosureService {
    void closeAsFalsePositive(UUID caseId, String rationale, UUID closedBy, String ip);
    void closeAfterStr(UUID caseId, UUID filingId, UUID closedBy, String ip);
}
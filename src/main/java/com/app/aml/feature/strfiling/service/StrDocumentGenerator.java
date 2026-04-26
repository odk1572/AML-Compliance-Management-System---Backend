package com.app.aml.feature.strfiling.service;


import com.app.aml.feature.casemanagement.entity.CaseNote;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.strfiling.entity.StrFiling;

import java.util.List;

public interface StrDocumentGenerator {
    byte[] generatePdf(StrFiling filing, List<Transaction> txns, List<CaseNote> notes, List<String> evidence);
    byte[] generateXml(StrFiling filing, List<Transaction> txns);
}
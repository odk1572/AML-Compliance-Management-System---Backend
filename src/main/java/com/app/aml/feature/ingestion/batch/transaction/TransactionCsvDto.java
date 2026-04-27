package com.app.aml.feature.ingestion.batch.transaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionCsvDto {
    private int lineNumber;

    private String transactionRef;
    private String originatorAccountNo;
    private String originatorName;
    private String originatorBankCode;
    private String originatorCountry;
    private String beneficiaryAccountNo;
    private String beneficiaryName;
    private String beneficiaryBankCode;
    private String beneficiaryCountry;
    private String amount;
    private String currencyCode;
    private String transactionType;
    private String channel;
    private String transactionTimestamp;
    private String referenceNote;

    private String status;
}
package com.app.aml.domain.enums;

public enum TransactionType {
    CASH,
    TRANSFER,
    NEFT,   // <--- Add this
    RTGS,   // <--- Add this
    IMPS,   // <--- Add this
    CARD,   // <--- Add this
    CHEQUE,
    DEPOSIT,
    WITHDRAWAL
}
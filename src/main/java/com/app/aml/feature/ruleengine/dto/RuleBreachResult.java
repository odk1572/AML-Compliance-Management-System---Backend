package com.app.aml.feature.ruleengine.dto;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
public class RuleBreachResult {
    private CustomerProfile customer;
    private List<Transaction> transactions;
    private String ruleType; 
    private String ruleLabel;
}
package com.app.aml.feature.ingestion.batch.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.batch.item.ItemCountAware;

/**
 * DTO representing a raw row from the Customer Profile CSV.
 * All fields are kept as String to prevent FlatFileItemReader from crashing on malformed data,
 * allowing the ValidationProcessor to handle errors gracefully.
 */
@Data
public class CustomerProfileCsvDto implements ItemCountAware {
    private int lineNumber;
    private String accountNumber;
    private String customerName;
    private String customerType;
    private String idType;
    private String idNumber;
    private String nationality;
    private String countryOfResidence;
    private String monthlyIncome;
    private String netWorth;
    private String riskRating;
    private String riskScore;        // <-- ADDED
    private String isPep;
    private String isDormant;
    private String accountOpenedOn;
    private String lastActivityDate; // <-- ADDED
    private String kycStatus;

    @Override
    public void setItemCount(int count) {
        this.lineNumber = count;
    }
}
package com.app.aml.feature.ingestion.batch.customer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.batch.item.ItemCountAware;

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
    private String riskScore;
    private String isPep;
    private String isDormant;
    private String accountOpenedOn;
    private String lastActivityDate;
    private String kycStatus;

    @Override
    public void setItemCount(int count) {
        this.lineNumber = count;
    }
}
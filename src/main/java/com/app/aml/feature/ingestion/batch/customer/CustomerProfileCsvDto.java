package com.app.aml.feature.ingestion.batch.customer;

import lombok.Data;
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
    private String isPep;
    private String isDormant;
    private String accountOpenedOn;

    @Override
    public void setItemCount(int count) {
        this.lineNumber = count;
    }
}
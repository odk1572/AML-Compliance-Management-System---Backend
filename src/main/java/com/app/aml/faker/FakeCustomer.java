package com.app.aml.faker;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FakeCustomer {
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

    // Helper method to convert the object to a CSV row
    public String toCsvRow() {
        return String.join(",",
                accountNumber, customerName, customerType, idType, idNumber,
                nationality, countryOfResidence, monthlyIncome, netWorth,
                riskRating, riskScore, isPep, isDormant, accountOpenedOn,
                lastActivityDate, kycStatus);
    }
}
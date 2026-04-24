package com.app.aml.faker;


import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CustomerCsvGenerator {

    // Using an Indian locale for realistic names and companies
    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private static final String HEADER = "accountNumber,customerName,customerType,idType,idNumber,nationality,countryOfResidence,monthlyIncome,netWorth,riskRating,riskScore,isPep,isDormant,accountOpenedOn,lastActivityDate,kycStatus\n";

    public byte[] generate(int count) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);

        for (int i = 1; i <= count; i++) {
            String[] row = buildRow(i);
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String[] buildRow(int index) {
        // 1. Account Number
        String accountNumber = "ACCT" + faker.number().digits(8);

        // 2 & 3. Customer Type and Name
        boolean isCorporate = random.nextInt(100) < 20; // 20% Corporate
        String customerType = isCorporate ? "CORPORATE" : "INDIVIDUAL";
        String customerName = isCorporate ? faker.company().name() : faker.name().fullName();
        // Escape commas in names for CSV
        if (customerName.contains(",")) {
            customerName = "\"" + customerName + "\"";
        }

        // 4 & 5. ID Data
        String[] idTypes = isCorporate ? new String[]{"CIN", "PAN", "LEI"} : new String[]{"AADHAAR", "PAN", "PASSPORT", "VOTER_ID"};
        String idType = idTypes[random.nextInt(idTypes.length)];
        String idNumber = faker.regexify("[A-Z0-9]{10}");

        // 6 & 7. Geography (ISO3 Codes)
        String nationality = "IND";
        String countryOfResidence = "IND";
        if (random.nextInt(100) < 15) { // 15% Expat/Foreign
            nationality = faker.country().currencyCode(); // gives 3 letter codes mostly
            countryOfResidence = faker.country().currencyCode();
        }

        // 8 & 9. Financials
        double monthlyIncomeBase = 1000 + (49000 * random.nextDouble());
        double netWorthBase = 10000 + (490000 * random.nextDouble());

        // 10 & 11. Risk Rating & Score Distribution
        int riskRoll = random.nextInt(100);
        String riskRating;
        int riskScore;
        if (riskRoll < 70) {
            riskRating = "LOW";
            riskScore = random.nextInt(26); // 0-25
        } else if (riskRoll < 90) {
            riskRating = "MEDIUM";
            riskScore = 26 + random.nextInt(25); // 26-50
        } else if (riskRoll < 98) {
            riskRating = "HIGH";
            riskScore = 51 + random.nextInt(25); // 51-75
        } else {
            riskRating = "CRITICAL";
            riskScore = 76 + random.nextInt(25); // 76-100
        }

        // 12 & 13. Flags
        boolean isPep = random.nextInt(100) < 5; // 5% PEP
        boolean isDormant = random.nextInt(100) < 10; // 10% Dormant

        // 14 & 15. Dates
        LocalDate accountOpenedOn = LocalDate.ofInstant(faker.date().past(3650, TimeUnit.DAYS).toInstant(), ZoneId.systemDefault());
        LocalDate lastActivityDate;
        if (isDormant) {
            // Dormant accounts haven't had activity in at least a year
            lastActivityDate = accountOpenedOn.plusDays(random.nextInt(Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(accountOpenedOn, LocalDate.now().minusYears(1)))));
        } else {
            // Active accounts had activity in the last 60 days
            lastActivityDate = LocalDate.now().minusDays(random.nextInt(60));
        }

        // 16. KYC Status
        String[] kycStatuses = {"APPROVED", "PENDING", "REJECTED"};
        String kycStatus = kycStatuses[random.nextInt(100) < 85 ? 0 : random.nextInt(3)]; // 85% Approved

        // ==========================================
        // EDGE CASE INJECTIONS (Data anomalies to test rules)
        // ==========================================

        // Edge Case 1: FATF Blacklisted Jurisdiction (e.g., North Korea, Iran) -> Forced Critical
        if (index % 47 == 0) {
            nationality = "PRK";
            countryOfResidence = "IRN";
            riskRating = "CRITICAL";
            riskScore = 99;
        }

        // Edge Case 2: Ultra-High Net Worth PEP with Low Income (Corruption Typology)
        if (index % 83 == 0) {
            isPep = true;
            monthlyIncomeBase = 500.00; // Unusually low
            netWorthBase = 95000000.00; // $95 Million
            riskRating = "HIGH";
        }

        // Edge Case 3: Contradictory Dormancy (Data inconsistency testing)
        // Flagged dormant, but last activity was yesterday
        if (index % 113 == 0) {
            isDormant = true;
            lastActivityDate = LocalDate.now().minusDays(1);
        }

        // Edge Case 4: Missing Critical KYC Information
        if (index % 137 == 0) {
            idType = "";
            idNumber = "";
            kycStatus = "PENDING";
            // A pending KYC account should ideally be restricted, testing your engine's limits
        }

        // Edge Case 5: Zero Income / Zero Net Worth (Shell Company Typology)
        if (index % 167 == 0) {
            isCorporate = true;
            customerType = "CORPORATE";
            monthlyIncomeBase = 0.00;
            netWorthBase = 0.00;
            accountOpenedOn = LocalDate.now().minusDays(2); // Just opened
        }

        // Format Financials
        String monthlyIncome = BigDecimal.valueOf(monthlyIncomeBase).setScale(2, RoundingMode.HALF_UP).toString();
        String netWorth = BigDecimal.valueOf(netWorthBase).setScale(2, RoundingMode.HALF_UP).toString();

        return new String[]{
                accountNumber,
                customerName,
                customerType,
                idType,
                idNumber,
                nationality,
                countryOfResidence,
                monthlyIncome,
                netWorth,
                riskRating,
                String.valueOf(riskScore),
                String.valueOf(isPep),
                String.valueOf(isDormant),
                accountOpenedOn.format(DATE_FORMATTER),
                lastActivityDate != null ? lastActivityDate.format(DATE_FORMATTER) : "",
                kycStatus
        };
    }
}
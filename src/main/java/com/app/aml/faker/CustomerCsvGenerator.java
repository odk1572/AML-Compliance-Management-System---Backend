package com.app.aml.faker;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class CustomerCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String HEADER = "accountNumber,customerName,customerType,idType,idNumber,nationality,countryOfResidence,monthlyIncome,netWorth,riskRating,riskScore,isPep,isDormant,accountOpenedOn,lastActivityDate,kycStatus\n";

    // 1. Generate the Objects in Memory
    public List<FakeCustomer> generateCustomerList(int count) {
        List<FakeCustomer> customers = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            customers.add(buildCustomer(i));
        }
        return customers;
    }

    // 2. Convert Objects to CSV Bytes
    public byte[] generateCsvBytes(List<FakeCustomer> customers) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);
        for (FakeCustomer c : customers) {
            csvBuilder.append(c.toCsvRow()).append("\n");
        }
        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private FakeCustomer buildCustomer(int index) {
        String accountNumber = "ACCT" + faker.number().digits(8);

        boolean isCorporate = random.nextInt(100) < 20;
        String customerType = isCorporate ? "CORPORATE" : "INDIVIDUAL";
        String customerName = isCorporate ? faker.company().name() : faker.name().fullName();
        if (customerName.contains(",")) customerName = "\"" + customerName + "\"";

        String[] idTypes = isCorporate ? new String[]{"CIN", "PAN", "LEI"} : new String[]{"AADHAAR", "PAN", "PASSPORT", "VOTER_ID"};
        String idType = idTypes[random.nextInt(idTypes.length)];
        String idNumber = faker.regexify("[A-Z0-9]{10}"); // Generic alphanumeric placeholder

        String nationality = "IND";
        String countryOfResidence = "IND";
        if (random.nextInt(100) < 15) {
            nationality = faker.country().currencyCode();
            countryOfResidence = faker.country().currencyCode();
        }

        double monthlyIncomeBase = 1000 + (49000 * random.nextDouble());
        double netWorthBase = 10000 + (490000 * random.nextDouble());

        int riskRoll = random.nextInt(100);
        String riskRating = riskRoll < 70 ? "LOW" : riskRoll < 90 ? "MEDIUM" : riskRoll < 98 ? "HIGH" : "CRITICAL";
        int riskScore = riskRoll < 70 ? random.nextInt(26) : riskRoll < 90 ? 26 + random.nextInt(25) : riskRoll < 98 ? 51 + random.nextInt(25) : 76 + random.nextInt(25);

        boolean isPep = random.nextInt(100) < 5;
        boolean isDormant = random.nextInt(100) < 10;

        LocalDate accountOpenedOn = LocalDate.ofInstant(faker.date().past(3650, TimeUnit.DAYS).toInstant(), ZoneId.systemDefault());
        LocalDate lastActivityDate = isDormant
                ? accountOpenedOn.plusDays(random.nextInt(Math.max(1, (int) java.time.temporal.ChronoUnit.DAYS.between(accountOpenedOn, LocalDate.now().minusYears(1)))))
                : LocalDate.now().minusDays(random.nextInt(60));

        String[] kycStatuses = {"APPROVED", "PENDING", "REJECTED"};
        String kycStatus = kycStatuses[random.nextInt(100) < 85 ? 0 : random.nextInt(3)];

        // Edge Cases (same as your original logic)
        if (index % 47 == 0) { nationality = "PRK"; countryOfResidence = "IRN"; riskRating = "CRITICAL"; riskScore = 99; }
        if (index % 83 == 0) { isPep = true; monthlyIncomeBase = 500.00; netWorthBase = 95000000.00; riskRating = "HIGH"; }
        if (index % 113 == 0) { isDormant = true; lastActivityDate = LocalDate.now().minusDays(1); }
        if (index % 137 == 0) { idType = ""; idNumber = ""; kycStatus = "PENDING"; }
        if (index % 167 == 0) { isCorporate = true; customerType = "CORPORATE"; monthlyIncomeBase = 0.00; netWorthBase = 0.00; accountOpenedOn = LocalDate.now().minusDays(2); }

        return FakeCustomer.builder()
                .accountNumber(accountNumber)
                .customerName(customerName)
                .customerType(customerType)
                .idType(idType)
                .idNumber(idNumber)
                .nationality(nationality)
                .countryOfResidence(countryOfResidence)
                .monthlyIncome(BigDecimal.valueOf(monthlyIncomeBase).setScale(2, RoundingMode.HALF_UP).toString())
                .netWorth(BigDecimal.valueOf(netWorthBase).setScale(2, RoundingMode.HALF_UP).toString())
                .riskRating(riskRating)
                .riskScore(String.valueOf(riskScore))
                .isPep(String.valueOf(isPep))
                .isDormant(String.valueOf(isDormant))
                .accountOpenedOn(accountOpenedOn.format(DATE_FORMATTER))
                .lastActivityDate(lastActivityDate != null ? lastActivityDate.format(DATE_FORMATTER) : "")
                .kycStatus(kycStatus)
                .build();
    }
}
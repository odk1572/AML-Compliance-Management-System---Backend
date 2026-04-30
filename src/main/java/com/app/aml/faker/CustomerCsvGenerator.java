package com.app.aml.faker;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class CustomerCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final String HEADER = "accountNumber,customerName,customerType,idType,idNumber,nationality,countryOfResidence,monthlyIncome,netWorth,riskRating,riskScore,isPep,isDormant,accountOpenedOn,lastActivityDate,kycStatus\n";

    private static final LocalDate START_DATE = LocalDate.of(2026, 1, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 4, 30);

    public List<FakeCustomer> generateCustomerList(int count) {
        List<FakeCustomer> customers = new ArrayList<>();

        // Updated Personas to fit strictly within the Jan 1 2026 - Apr 30 2026 window
        customers.add(buildPersona("BRK-VELOCITY-01", "Velocity Breaker LLC", "CORPORATE", "HIGH", "15000.00", false, LocalDate.of(2026, 1, 10)));
        customers.add(buildPersona("BRK-STRUCT-01", "Structuring Smurf Inc", "INDIVIDUAL", "HIGH", "5000.00", false, LocalDate.of(2026, 2, 1)));
        customers.add(buildPersona("BRK-DORMANT-01", "Rip Van Winkle", "INDIVIDUAL", "MEDIUM", "2000.00", true, LocalDate.of(2026, 1, 5)));
        customers.add(buildPersona("BRK-LOW-INCOME-01", "Low Income High Vol", "INDIVIDUAL", "CRITICAL", "800.00", false, LocalDate.of(2026, 3, 15)));
        customers.add(buildPersona("BRK-SPIKE-01", "Steady Eddy", "INDIVIDUAL", "LOW", "4000.00", false, LocalDate.of(2026, 1, 20)));

        for (int i = customers.size() + 1; i <= count; i++) {
            customers.add(buildRandomCustomer(i));
        }

        return customers;
    }

    public byte[] generateCsvBytes(List<FakeCustomer> customers) {
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);
        for (FakeCustomer c : customers) {
            csvBuilder.append(c.toCsvRow()).append("\n");
        }
        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private FakeCustomer buildRandomCustomer(int index) {
        String accountNumber = "ACCT" + faker.number().digits(8);
        boolean isCorporate = random.nextInt(100) < 20;
        String customerType = isCorporate ? "CORPORATE" : "INDIVIDUAL";
        String customerName = isCorporate ? faker.company().name() : faker.name().fullName();
        if (customerName.contains(",")) customerName = "\"" + customerName + "\"";

        String[] idTypes = isCorporate ? new String[]{"CIN", "PAN", "LEI"} : new String[]{"AADHAAR", "PAN", "PASSPORT", "VOTER_ID"};
        String idType = idTypes[random.nextInt(idTypes.length)];
        String idNumber = faker.regexify("[A-Z0-9]{10}");

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


        long totalDaysInWindow = ChronoUnit.DAYS.between(START_DATE, END_DATE);
        LocalDate accountOpenedOn = START_DATE.plusDays(random.nextInt((int) totalDaysInWindow + 1));

        LocalDate lastActivityDate;
        if (isDormant) {
            long maxDormantDays = Math.min(5, ChronoUnit.DAYS.between(accountOpenedOn, END_DATE));
            lastActivityDate = accountOpenedOn.plusDays(maxDormantDays > 0 ? random.nextInt((int) maxDormantDays + 1) : 0);
        } else {
            long activeDays = ChronoUnit.DAYS.between(accountOpenedOn, END_DATE);
            lastActivityDate = accountOpenedOn.plusDays(activeDays > 0 ? random.nextInt((int) activeDays + 1) : 0);
        }

        String[] kycStatuses = {"APPROVED", "PENDING", "REJECTED"};
        String kycStatus = kycStatuses[random.nextInt(100) < 85 ? 0 : random.nextInt(3)];

        if (index % 47 == 0) { nationality = "PRK"; countryOfResidence = "IRN"; riskRating = "CRITICAL"; riskScore = 99; }
        if (index % 83 == 0) { isPep = true; monthlyIncomeBase = 500.00; netWorthBase = 95000000.00; riskRating = "HIGH"; }
        if (index % 137 == 0) { idType = ""; idNumber = ""; kycStatus = "PENDING"; }

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
                .lastActivityDate(lastActivityDate.format(DATE_FORMATTER))
                .kycStatus(kycStatus)
                .build();
    }

    private FakeCustomer buildPersona(String acct, String name, String type, String risk, String income, boolean dormant, LocalDate opened) {
        LocalDate lastActivity;
        if (dormant) {
            lastActivity = opened.plusDays(3);
            if (lastActivity.isAfter(END_DATE)) lastActivity = END_DATE;
        } else {
            lastActivity = END_DATE.minusDays(random.nextInt(10));
            if (lastActivity.isBefore(opened)) lastActivity = opened;
        }

        return FakeCustomer.builder()
                .accountNumber(acct)
                .customerName(name)
                .customerType(type)
                .idType("PAN")
                .idNumber(faker.regexify("[A-Z]{5}[0-9]{4}[A-Z]{1}"))
                .nationality("IND")
                .countryOfResidence("IND")
                .monthlyIncome(income)
                .netWorth("500000.00")
                .riskRating(risk)
                .riskScore(risk.equals("CRITICAL") ? "95" : "75")
                .isPep("false")
                .isDormant(String.valueOf(dormant))
                .accountOpenedOn(opened.format(DATE_FORMATTER))
                .lastActivityDate(lastActivity.format(DATE_FORMATTER))
                .kycStatus("APPROVED")
                .build();
    }
}
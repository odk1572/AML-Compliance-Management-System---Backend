package com.app.aml.faker;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class StructuringTransactionCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();

    private static final String HEADER = "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry,beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry,amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status\n";

    private static final String[] LOW_RISK_COUNTRIES = {"IND", "USA", "GBR", "SGP", "ARE"};
    private static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP", "AED"};
    private static final String[] TXN_TYPES = {"WIRE", "ACH", "CARD", "CASH", "RTGS", "NEFT"};
    private static final String[] CHANNELS = {"ONLINE", "BRANCH", "MOBILE", "UPI", "ATM"};

    public byte[] generate(int noiseCount, List<FakeCustomer> customers) {
        if (customers == null || customers.isEmpty()) {
            throw new IllegalArgumentException("Customer pool cannot be empty.");
        }

        List<String[]> allTransactions = new ArrayList<>();

        // 1. Generate normal background noise
        for (int i = 1; i <= noiseCount; i++) {
            allTransactions.add(buildNoiseRow(i, customers));
        }

        // 2. Inject the specific Structuring Breach
        allTransactions.addAll(injectStructuringBreach(customers));

        // 3. Compile to CSV
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);
        for (String[] row : allTransactions) {
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    private List<String[]> injectStructuringBreach(List<FakeCustomer> customers) {
        List<String[]> breachTxns = new ArrayList<>();

        // Find our target persona, or fallback to the first customer
        FakeCustomer target = customers.stream()
                .filter(c -> c.getAccountNumber().equals("BRK-STRUCT-01"))
                .findFirst()
                .orElse(customers.get(0));

        OffsetDateTime windowEnd = OffsetDateTime.now().minus(1, ChronoUnit.DAYS);

        // Generate 8 transactions, spaced 6 hours apart (Total window: 48 hours)
        // Each transaction is exactly $9,500 (Usually below the $10k reporting limit)
        for (int i = 0; i < 8; i++) {
            FakeCustomer receiver = getRandomCustomer(customers, target);
            OffsetDateTime timestamp = windowEnd.minus(i * 6L, ChronoUnit.HOURS);

            // Add a slight randomization to the amount so it looks organic ($9,400 to $9,900)
            double amount = 9400.00 + (500.00 * random.nextDouble());
            String formattedAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toString();

            // Structuring is almost always done via CASH at a BRANCH or ATM
            String channel = random.nextBoolean() ? "BRANCH" : "ATM";

            breachTxns.add(createCustomTxn(target, receiver, formattedAmount, timestamp, "CASH", channel));
        }

        return breachTxns;
    }

    private String[] createCustomTxn(FakeCustomer orig, FakeCustomer ben, String amount, OffsetDateTime timestamp, String type, String channel) {
        String ref = "TXN-STRUCT-" + System.currentTimeMillis() + random.nextInt(9999);
        return new String[]{
                ref, orig.getAccountNumber(), orig.getCustomerName(), "OUR_BANK", orig.getCountryOfResidence(),
                ben.getAccountNumber(), ben.getCustomerName(), "OUR_BANK", ben.getCountryOfResidence(),
                amount, "USD", type, channel, timestamp.toString(), "Cash Deposit", "CLEAN"
        };
    }

    private FakeCustomer getRandomCustomer(List<FakeCustomer> customers, FakeCustomer exclude) {
        FakeCustomer c = customers.get(random.nextInt(customers.size()));
        while (exclude != null && c.getAccountNumber().equals(exclude.getAccountNumber())) {
            c = customers.get(random.nextInt(customers.size()));
        }
        return c;
    }

    // ====================================================================
    // BACKGROUND NOISE GENERATOR
    // ====================================================================
    private String[] buildNoiseRow(int index, List<FakeCustomer> customers) {
        String transactionRef = "TXN" + System.currentTimeMillis() + faker.number().digits(6);
        FakeCustomer primaryCustomer = customers.get(random.nextInt(customers.size()));

        String originatorAccountNo = primaryCustomer.getAccountNumber();
        String originatorName = primaryCustomer.getCustomerName();
        String originatorCountry = primaryCustomer.getCountryOfResidence();

        FakeCustomer secondaryCustomer = getRandomCustomer(customers, primaryCustomer);
        String beneficiaryAccountNo = secondaryCustomer.getAccountNumber();
        String beneficiaryName = secondaryCustomer.getCustomerName();
        String beneficiaryCountry = secondaryCustomer.getCountryOfResidence();

        String currencyCode = CURRENCIES[random.nextInt(CURRENCIES.length)];

        double amountBase = random.nextBoolean() ? (100 + (3000 * random.nextDouble())) : (15000 + (50000 * random.nextDouble()));
        String amount = BigDecimal.valueOf(amountBase).setScale(2, RoundingMode.HALF_UP).toString();

        String transactionType = TXN_TYPES[random.nextInt(TXN_TYPES.length)];
        String channel = CHANNELS[random.nextInt(CHANNELS.length)];

        if (channel.equals("UPI")) transactionType = "WIRE";
        if (channel.equals("ATM")) transactionType = "CASH";

        OffsetDateTime transactionTimestamp = OffsetDateTime.now()
                .minus(random.nextInt(90), ChronoUnit.DAYS)
                .minus(random.nextInt(24), ChronoUnit.HOURS);

        return new String[]{
                transactionRef, originatorAccountNo, originatorName, "OUR_BANK", originatorCountry,
                beneficiaryAccountNo, beneficiaryName, "OUR_BANK", beneficiaryCountry,
                amount, currencyCode, transactionType, channel, transactionTimestamp.toString(), "Standard Transfer", "CLEAN"
        };
    }
}
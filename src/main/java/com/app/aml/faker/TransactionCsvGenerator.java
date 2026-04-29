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
public class TransactionCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();

    private static final String HEADER = "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry,beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry,amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status\n";

    private static final String[] BLACKLIST_COUNTRIES = {"PRK", "IRN", "MMR"};
    private static final String[] LOW_RISK_COUNTRIES = {"IND", "USA", "GBR", "SGP", "ARE"};
    private static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP", "AED"};
    private static final String[] TXN_TYPES = {"WIRE", "ACH", "CARD", "CASH", "RTGS", "NEFT"};
    private static final String[] CHANNELS = {"ONLINE", "BRANCH", "MOBILE", "UPI", "ATM"};

    private static final String[] STRATEGIES = {
            "STRUCTURING", "VELOCITY", "LARGE_TRANSACTION", "ROUND_AMOUNT",
            "PASS_THROUGH", "FUNNEL", "SUDDEN_SPIKE", "DORMANT_REACTIVATION", "LOW_INCOME_HIGH_TRANSFER"
    };

    public byte[] generate(int noiseCount, List<FakeCustomer> customers) {
        if (customers == null || customers.size() < 5) {
            throw new IllegalArgumentException("Customer pool must contain at least 5 profiles for complex typologies.");
        }

        List<String[]> allTransactions = new ArrayList<>();

        // 1. Generate normal background noise
        for (int i = 1; i <= noiseCount; i++) {
            allTransactions.add(buildNoiseRow(i, customers));
        }

        // 2. Inject Random Breaches
        // Let's guarantee ALL strategies run at least once for thorough testing
        for (String strategy : STRATEGIES) {
            allTransactions.addAll(injectBreach(strategy, customers));
        }

        // 3. Compile to CSV
        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);
        for (String[] row : allTransactions) {
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }


    private List<String[]> injectBreach(String strategy, List<FakeCustomer> customers) {
        List<String[]> breachTxns = new ArrayList<>();
        FakeCustomer primary = getRandomCustomer(customers, null);

        OffsetDateTime now = OffsetDateTime.now();

        switch (strategy) {
            case "VELOCITY" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-VELOCITY-01", primary);
                for (int i = 0; i < 15; i++) {
                    breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target), "500.00", now.minus(i, ChronoUnit.MINUTES), "ATM"));
                }
            }
            case "STRUCTURING" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-STRUCT-01", primary);
                for (int i = 0; i < 8; i++) {
                    breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target), "9000.00", now.minus(i * 5L, ChronoUnit.HOURS), "CASH"));
                }
            }
            case "DORMANT_REACTIVATION" -> {
                // 1 large transaction exactly 1 hour ago for a previously sleeping account
                FakeCustomer target = findSpecificTarget(customers, "BRK-DORMANT-01", primary);
                breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target), "25000.00", now.minus(1, ChronoUnit.HOURS), "WIRE"));
            }
            case "LOW_INCOME_HIGH_TRANSFER" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-LOW-INCOME-01", primary);
                breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target), "50000.00", now.minus(30, ChronoUnit.MINUTES), "WIRE"));
            }
            case "SUDDEN_SPIKE" -> {

                FakeCustomer target = findSpecificTarget(customers, "BRK-SPIKE-01", primary);
                breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target), "95000.00", now.minus(12, ChronoUnit.HOURS), "RTGS"));
            }
            case "PASS_THROUGH" -> {
                FakeCustomer middleMan = primary;
                FakeCustomer sender = getRandomCustomer(customers, middleMan);
                FakeCustomer ultimateReceiver = getRandomCustomer(customers, middleMan);
                breachTxns.add(createCustomTxn(sender, middleMan, "80000.00", now.minus(2, ChronoUnit.HOURS), "WIRE"));
                breachTxns.add(createCustomTxn(middleMan, ultimateReceiver, "79950.00", now.minus(1, ChronoUnit.HOURS), "WIRE")); // Leaves $50
            }
            case "FUNNEL" -> {
                FakeCustomer funnelTarget = primary;
                for (int i = 0; i < 4; i++) {
                    FakeCustomer sender = getRandomCustomer(customers, funnelTarget);
                    breachTxns.add(createCustomTxn(sender, funnelTarget, "9500.00", now.minus(i * 30L, ChronoUnit.MINUTES), "ONLINE"));
                }
            }
            case "LARGE_TRANSACTION" ->
                    breachTxns.add(createCustomTxn(primary, getRandomCustomer(customers, primary), "750000.00", now.minus(2, ChronoUnit.DAYS), "WIRE"));
            case "ROUND_AMOUNT" ->
                    breachTxns.add(createCustomTxn(primary, getRandomCustomer(customers, primary), "50000.00", now.minus(5, ChronoUnit.DAYS), "ONLINE"));
        }
        return breachTxns;
    }



    private String[] createCustomTxn(FakeCustomer orig, FakeCustomer ben, String amount, OffsetDateTime timestamp, String type) {
        String ref = "TXN-BRCH-" + System.currentTimeMillis() + random.nextInt(999);
        return new String[]{
                ref, orig.getAccountNumber(), orig.getCustomerName(), "OUR_BANK", orig.getCountryOfResidence(),
                ben.getAccountNumber(), ben.getCustomerName(), "OUR_BANK", ben.getCountryOfResidence(),
                amount, "USD", type, "ONLINE", timestamp.toString(), "Suspicious Activity", "CLEAN"
        };
    }

    private FakeCustomer findSpecificTarget(List<FakeCustomer> customers, String targetAcct, FakeCustomer fallback) {
        return customers.stream()
                .filter(c -> c.getAccountNumber().equals(targetAcct))
                .findFirst()
                .orElse(fallback);
    }

    private FakeCustomer getRandomCustomer(List<FakeCustomer> customers, FakeCustomer exclude) {
        FakeCustomer c = customers.get(random.nextInt(customers.size()));
        while (exclude != null && c.getAccountNumber().equals(exclude.getAccountNumber())) {
            c = customers.get(random.nextInt(customers.size()));
        }
        return c;
    }


    private String[] buildNoiseRow(int index, List<FakeCustomer> customers) {
        String transactionRef = "TXN" + System.currentTimeMillis() + faker.number().digits(6);
        FakeCustomer primaryCustomer = customers.get(random.nextInt(customers.size()));

        String originatorAccountNo, originatorName, originatorBankCode, originatorCountry;
        String beneficiaryAccountNo, beneficiaryName, beneficiaryBankCode, beneficiaryCountry;

        boolean isInternalTransfer = random.nextInt(100) < 60;

        if (isInternalTransfer) {
            FakeCustomer secondaryCustomer = getRandomCustomer(customers, primaryCustomer);

            originatorAccountNo = primaryCustomer.getAccountNumber();
            originatorName = primaryCustomer.getCustomerName();
            originatorCountry = primaryCustomer.getCountryOfResidence();
            originatorBankCode = "OUR_BANK";

            beneficiaryAccountNo = secondaryCustomer.getAccountNumber();
            beneficiaryName = secondaryCustomer.getCustomerName();
            beneficiaryCountry = secondaryCustomer.getCountryOfResidence();
            beneficiaryBankCode = "OUR_BANK";
        } else {
            boolean isOutgoing = random.nextBoolean();
            String externalAccount = "EXT" + faker.number().digits(10);
            String externalName = faker.company().name().replace(",", "");
            String externalCountry = LOW_RISK_COUNTRIES[random.nextInt(LOW_RISK_COUNTRIES.length)];
            String externalBank = faker.finance().bic();

            if (isOutgoing) {
                originatorAccountNo = primaryCustomer.getAccountNumber();
                originatorName = primaryCustomer.getCustomerName();
                originatorCountry = primaryCustomer.getCountryOfResidence();
                originatorBankCode = "OUR_BANK";

                beneficiaryAccountNo = externalAccount;
                beneficiaryName = externalName;
                beneficiaryCountry = externalCountry;
                beneficiaryBankCode = externalBank;
            } else {
                originatorAccountNo = externalAccount;
                originatorName = externalName;
                originatorCountry = externalCountry;
                originatorBankCode = externalBank;

                beneficiaryAccountNo = primaryCustomer.getAccountNumber();
                beneficiaryName = primaryCustomer.getCustomerName();
                beneficiaryCountry = primaryCustomer.getCountryOfResidence();
                beneficiaryBankCode = "OUR_BANK";
            }
        }

        int currRoll = random.nextInt(100);
        String currencyCode = currRoll < 60 ? "INR" : currRoll < 85 ? "USD" : CURRENCIES[random.nextInt(CURRENCIES.length)];


        double amountBase = 100 + (8000 * random.nextDouble());

        String transactionType = TXN_TYPES[random.nextInt(TXN_TYPES.length)];
        String channel = CHANNELS[random.nextInt(CHANNELS.length)];

        if (index % 31 == 0) { amountBase = 9900 + (99 * random.nextDouble()); channel = "CASH"; }
        if (random.nextInt(100) < 8) { amountBase = 10000 + (25000 * random.nextDouble()); }
        if (random.nextInt(100) < 5) {
            int[] roundMultipliers = {1000, 5000};
            amountBase = roundMultipliers[random.nextInt(roundMultipliers.length)] * (1 + random.nextInt(5));
        }

        if (!isInternalTransfer && index % 43 == 0) {
            if (originatorBankCode.equals("OUR_BANK")) beneficiaryCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
            else originatorCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
        }

        if (channel.equals("UPI")) transactionType = "WIRE";
        if (channel.equals("ATM")) transactionType = "CASH";

        // Generate background transactions using OffsetDateTime correctly
        OffsetDateTime transactionTimestamp = OffsetDateTime.now()
                .minus(random.nextInt(90), ChronoUnit.DAYS)
                .minus(random.nextInt(24), ChronoUnit.HOURS);

        String referenceNote = "";
        if (index % 89 == 0) referenceNote = "Loan payment";
        else if (index % 103 == 0) referenceNote = "Consulting fees";
        else if (random.nextBoolean()) referenceNote = "TXN REF " + faker.number().digits(4);

        String amount = BigDecimal.valueOf(amountBase).setScale(2, RoundingMode.HALF_UP).toString();

        return new String[]{
                transactionRef, originatorAccountNo, originatorName, originatorBankCode, originatorCountry,
                beneficiaryAccountNo, beneficiaryName, beneficiaryBankCode, beneficiaryCountry,
                amount, currencyCode, transactionType, channel, transactionTimestamp.toString(), referenceNote, "CLEAN"
        };
    }
}
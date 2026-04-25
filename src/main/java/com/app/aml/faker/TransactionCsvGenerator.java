package com.app.aml.faker;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@Service
public class TransactionCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();

    private static final String HEADER = "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry,beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry,amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status\n";

    private static final String[] BLACKLIST_COUNTRIES = {"PRK", "IRN", "MMR"};
    private static final String[] GREYLIST_COUNTRIES = {"SYR", "YEM", "SSD", "HTI"};
    private static final String[] LOW_RISK_COUNTRIES = {"IND", "USA", "GBR", "SGP", "ARE"};
    private static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP", "AED"};
    private static final String[] TXN_TYPES = {"WIRE", "ACH", "CARD", "CASH", "RTGS", "NEFT"};
    private static final String[] CHANNELS = {"ONLINE", "BRANCH", "MOBILE", "UPI", "ATM"};

    public byte[] generate(int count, List<FakeCustomer> customers) {
        if (customers == null || customers.size() < 2) {
            throw new IllegalArgumentException("Customer pool must contain at least 2 profiles.");
        }

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);

        for (int i = 1; i <= count; i++) {
            String[] row = buildRow(i, customers);
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String[] buildRow(int index, List<FakeCustomer> customers) {
        String transactionRef = "TXN" + Instant.now().toEpochMilli() + faker.number().digits(6);

        // Pick a primary customer from our generated DB
        FakeCustomer primaryCustomer = customers.get(random.nextInt(customers.size()));

        String originatorAccountNo, originatorName, originatorBankCode, originatorCountry;
        String beneficiaryAccountNo, beneficiaryName, beneficiaryBankCode, beneficiaryCountry;

        // 60% of transactions are internal (between two of our generated customers)
        // 40% are external (in or out)
        boolean isInternalTransfer = random.nextInt(100) < 60;

        if (isInternalTransfer) {
            FakeCustomer secondaryCustomer = customers.get(random.nextInt(customers.size()));
            while (primaryCustomer.getAccountNumber().equals(secondaryCustomer.getAccountNumber())) {
                secondaryCustomer = customers.get(random.nextInt(customers.size()));
            }

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

        // --- Rest of your original logic (Amounts, Currencies, Dates, Edge Cases) ---
        int currRoll = random.nextInt(100);
        String currencyCode = currRoll < 60 ? "INR" : currRoll < 85 ? "USD" : CURRENCIES[random.nextInt(CURRENCIES.length)];
        double amountBase = 100 + (499900 * random.nextDouble());
        String transactionType = TXN_TYPES[random.nextInt(TXN_TYPES.length)];
        String channel = CHANNELS[random.nextInt(CHANNELS.length)];

        // Edge Cases
        if (index % 31 == 0) { amountBase = 9900 + (99 * random.nextDouble()); channel = "CASH"; }
        if (random.nextInt(100) < 8) { amountBase = 10000 + (490000 * random.nextDouble()); }
        if (random.nextInt(100) < 5) {
            int[] roundMultipliers = {1000, 5000, 10000, 50000};
            amountBase = roundMultipliers[random.nextInt(roundMultipliers.length)] * (1 + random.nextInt(10));
        }

        if (!isInternalTransfer && index % 43 == 0) {
            if (originatorBankCode.equals("OUR_BANK")) beneficiaryCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
            else originatorCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
        }

        if (channel.equals("UPI")) transactionType = "WIRE";
        if (channel.equals("ATM")) transactionType = "CASH";

        Instant transactionTimestamp = Instant.now().minus(random.nextInt(90), ChronoUnit.DAYS).minus(random.nextInt(24), ChronoUnit.HOURS);

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
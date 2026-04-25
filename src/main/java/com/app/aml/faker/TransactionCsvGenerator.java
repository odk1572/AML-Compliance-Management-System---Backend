package com.app.aml.faker;


import org.springframework.stereotype.Service;
import com.github.javafaker.Faker;

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
    private List<String> accountPool;

    private static final String HEADER = "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry,beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry,amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status\n";

    // High-Risk Geographies mapped from your DB script
    private static final String[] BLACKLIST_COUNTRIES = {"PRK", "IRN", "MMR"};
    private static final String[] GREYLIST_COUNTRIES = {"SYR", "YEM", "SSD", "HTI"};
    private static final String[] LOW_RISK_COUNTRIES = {"IND", "USA", "GBR", "SGP", "ARE"};

    private static final String[] CURRENCIES = {"INR", "USD", "EUR", "GBP", "AED"};
    private static final String[] TXN_TYPES = {"WIRE", "ACH", "CARD", "CASH", "RTGS", "NEFT"};
    private static final String[] CHANNELS = {"ONLINE", "BRANCH", "MOBILE", "UPI", "ATM"};

    public byte[] generate(int count, List<String> accountNumbers) {
        if (accountNumbers == null || accountNumbers.isEmpty()) {
            throw new IllegalArgumentException("Account pool cannot be empty. Must load Customer Profiles first.");
        }
        this.accountPool = accountNumbers;

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);

        for (int i = 1; i <= count; i++) {
            String[] row = buildRow(i);
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String[] buildRow(int index) {
        // 1. Transaction Ref
        String transactionRef = "TXN" + Instant.now().toEpochMilli() + faker.number().digits(6);

        // 2. Decide Direction (Is our customer sending or receiving?)
        boolean isOutgoing = random.nextBoolean();

        String internalAccount = accountPool.get(random.nextInt(accountPool.size()));
        String internalName = faker.name().fullName().replace(",", "");
        String externalAccount = "EXT" + faker.number().digits(10);
        String externalName = faker.company().name().replace(",", "");

        String originatorAccountNo = isOutgoing ? internalAccount : externalAccount;
        String originatorName = isOutgoing ? internalName : externalName;
        String originatorBankCode = isOutgoing ? "OUR_BANK" : faker.finance().bic();

        String beneficiaryAccountNo = !isOutgoing ? internalAccount : externalAccount;
        String beneficiaryName = !isOutgoing ? internalName : externalName;
        String beneficiaryBankCode = !isOutgoing ? "OUR_BANK" : faker.finance().bic();

        // 3. Geography & Currency defaults
        String originatorCountry = LOW_RISK_COUNTRIES[random.nextInt(LOW_RISK_COUNTRIES.length)];
        String beneficiaryCountry = LOW_RISK_COUNTRIES[random.nextInt(LOW_RISK_COUNTRIES.length)];

        // Weight currencies: 60% INR, 25% USD, 15% Others
        int currRoll = random.nextInt(100);
        String currencyCode = currRoll < 60 ? "INR" : currRoll < 85 ? "USD" : CURRENCIES[random.nextInt(CURRENCIES.length)];

        // 4. Base Amount Logic (100 to 500,000)
        double amountBase = 100 + (499900 * random.nextDouble());

        // 5. Types and Channels (MOVED UP HERE so edge cases can override them)
        String transactionType = TXN_TYPES[random.nextInt(TXN_TYPES.length)];
        String channel = CHANNELS[random.nextInt(CHANNELS.length)];

        // ==========================================
        // EDGE CASE INJECTIONS FOR AML RULES
        // ==========================================

        // Edge Case 1: Structuring / Smurfing (Amounts just below 10k reporting limit)
        // Happens ~3% of the time
        if (index % 31 == 0) {
            amountBase = 9900 + (99 * random.nextDouble()); // e.g., 9945.50, 9999.00
            channel = "CASH"; // Now this resolves perfectly!
        }

        // Edge Case 2: Large Transactions (8% of transactions > 10,000 as per prompt)
        if (random.nextInt(100) < 8) {
            amountBase = 10000 + (490000 * random.nextDouble());
        }

        // Edge Case 3: Round Amounts (5% of transactions)
        if (random.nextInt(100) < 5) {
            int[] roundMultipliers = {1000, 5000, 10000, 50000};
            amountBase = roundMultipliers[random.nextInt(roundMultipliers.length)] * (1 + random.nextInt(10));
        }

        // Edge Case 4: High-Risk Geography (Sanctions / Blacklist Testing)
        // Inject FATF Blacklist/Greylist countries to the EXTERNAL party
        if (index % 43 == 0) {
            if (isOutgoing) {
                beneficiaryCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
            } else {
                originatorCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
            }
        } else if (index % 29 == 0) {
            if (isOutgoing) {
                beneficiaryCountry = GREYLIST_COUNTRIES[random.nextInt(GREYLIST_COUNTRIES.length)];
            } else {
                originatorCountry = GREYLIST_COUNTRIES[random.nextInt(GREYLIST_COUNTRIES.length)];
            }
        }

        // Tie specific channels to types for realism
        if (channel.equals("UPI")) transactionType = "WIRE";
        if (channel.equals("ATM")) transactionType = "CASH";

        // 6. Timestamp (within last 90 days)
        Instant transactionTimestamp = Instant.now().minus(random.nextInt(90), ChronoUnit.DAYS)
                .minus(random.nextInt(24), ChronoUnit.HOURS)
                .minus(random.nextInt(60), ChronoUnit.MINUTES);

        // 7. Reference Notes (Sometimes empty, sometimes suspicious)
        String referenceNote = "";
        if (index % 89 == 0) {
            referenceNote = "Loan payment";
        } else if (index % 103 == 0) {
            referenceNote = "Consulting fees"; // Common front for money laundering
        } else if (random.nextBoolean()) {
            referenceNote = "TXN REF " + faker.number().digits(4);
        }

        // 8. Status
        String status = "CLEAN";

        // Format amount
        String amount = BigDecimal.valueOf(amountBase).setScale(2, RoundingMode.HALF_UP).toString();

        return new String[]{
                transactionRef,
                originatorAccountNo,
                originatorName,
                originatorBankCode,
                originatorCountry,
                beneficiaryAccountNo,
                beneficiaryName,
                beneficiaryBankCode,
                beneficiaryCountry,
                amount,
                currencyCode,
                transactionType,
                channel,
                transactionTimestamp.toString(),
                referenceNote,
                status
        };
    }
}
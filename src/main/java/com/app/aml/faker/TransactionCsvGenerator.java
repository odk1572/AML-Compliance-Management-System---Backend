package com.app.aml.faker;

import com.github.javafaker.Faker;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TransactionCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();

    // ✅ AtomicLong counter — no more duplicate refs from same millisecond
    private final AtomicLong refCounter = new AtomicLong(System.currentTimeMillis());

    private static final String HEADER =
            "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry," +
                    "beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry," +
                    "amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status\n";

    private static final String[] BLACKLIST_COUNTRIES = {"PRK", "IRN", "MMR"};
    private static final String[] LOW_RISK_COUNTRIES  = {"IND", "USA", "GBR", "SGP", "ARE"};
    private static final String[] CURRENCIES          = {"INR", "USD", "EUR", "GBP", "AED"};
    private static final String[] TXN_TYPES           = {"WIRE", "ACH", "CARD", "CASH", "RTGS", "NEFT"};
    private static final String[] CHANNELS            = {"ONLINE", "BRANCH", "MOBILE", "UPI", "ATM"};

    private static final String[] STRATEGIES = {
            "STRUCTURING", "VELOCITY", "LARGE_TRANSACTION", "ROUND_AMOUNT",
            "PASS_THROUGH", "FUNNEL", "SUDDEN_SPIKE", "DORMANT_REACTIVATION", "LOW_INCOME_HIGH_TRANSFER"
    };

    private static final OffsetDateTime START_DATE =
            OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END_DATE =
            OffsetDateTime.of(2026, 4, 30, 23, 59, 59, 0, ZoneOffset.UTC);

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    // ✅ Streaming version — writes directly to OutputStream, never builds full CSV in memory
    public void streamGenerate(int noiseCount, List<FakeCustomer> customers, OutputStream out) throws IOException {
        if (customers == null || customers.size() < 5) {
            throw new IllegalArgumentException("Customer pool must contain at least 5 profiles.");
        }

        out.write(HEADER.getBytes(StandardCharsets.UTF_8));

        // Write noise rows in batches of 500 — flush periodically to avoid buffer buildup
        final int FLUSH_EVERY = 500;
        StringBuilder buffer = new StringBuilder(64 * 1024); // 64KB buffer

        for (int i = 1; i <= noiseCount; i++) {
            appendRow(buffer, buildNoiseRow(i, customers));

            if (i % FLUSH_EVERY == 0) {
                out.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
                buffer.setLength(0); // ✅ reuse buffer instead of creating new strings
            }
        }

        // Write breach rows
        for (String strategy : STRATEGIES) {
            for (String[] row : injectBreach(strategy, customers)) {
                appendRow(buffer, row);
            }
        }

        // Flush remaining
        if (!buffer.isEmpty()) {
            out.write(buffer.toString().getBytes(StandardCharsets.UTF_8));
        }

        out.flush();
    }

    // ✅ Keep original generate() for backward compatibility with small counts (<50k)
    public byte[] generate(int noiseCount, List<FakeCustomer> customers) {
        if (customers == null || customers.size() < 5) {
            throw new IllegalArgumentException("Customer pool must contain at least 5 profiles.");
        }

        List<String[]> allTransactions = new ArrayList<>(noiseCount + 50);

        for (int i = 1; i <= noiseCount; i++) {
            allTransactions.add(buildNoiseRow(i, customers));
        }
        for (String strategy : STRATEGIES) {
            allTransactions.addAll(injectBreach(strategy, customers));
        }

        StringBuilder csvBuilder = new StringBuilder(allTransactions.size() * 200);
        csvBuilder.append(HEADER);
        for (String[] row : allTransactions) {
            appendRow(csvBuilder, row);
        }

        return csvBuilder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void appendRow(StringBuilder sb, String[] row) {
        for (int i = 0; i < row.length; i++) {
            if (i > 0) sb.append(',');
            String val = row[i];
            // ✅ Escape commas and quotes in values to prevent malformed CSV
            if (val != null && (val.contains(",") || val.contains("\""))) {
                sb.append('"').append(val.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(val != null ? val : "");
            }
        }
        sb.append('\n');
    }

    // ✅ Unique ref using AtomicLong — thread-safe, no duplicates
    private String nextRef(String prefix) {
        return prefix + refCounter.getAndIncrement();
    }

    private List<String[]> injectBreach(String strategy, List<FakeCustomer> customers) {
        List<String[]> breachTxns = new ArrayList<>();
        FakeCustomer primary = getRandomCustomer(customers, null);
        OffsetDateTime now = END_DATE;

        switch (strategy) {
            case "VELOCITY" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-VELOCITY-01", primary);
                for (int i = 0; i < 15; i++) {
                    breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target),
                            "500.00", now.minus(i, ChronoUnit.MINUTES), "ATM"));
                }
            }
            case "STRUCTURING" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-STRUCT-01", primary);
                for (int i = 0; i < 8; i++) {
                    breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target),
                            "9000.00", now.minus(i * 5L, ChronoUnit.HOURS), "CASH"));
                }
            }
            case "DORMANT_REACTIVATION" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-DORMANT-01", primary);
                breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target),
                        "25000.00", now.minus(1, ChronoUnit.HOURS), "WIRE"));
            }
            case "LOW_INCOME_HIGH_TRANSFER" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-LOW-INCOME-01", primary);
                breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target),
                        "50000.00", now.minus(30, ChronoUnit.MINUTES), "WIRE"));
            }
            case "SUDDEN_SPIKE" -> {
                FakeCustomer target = findSpecificTarget(customers, "BRK-SPIKE-01", primary);
                breachTxns.add(createCustomTxn(target, getRandomCustomer(customers, target),
                        "95000.00", now.minus(12, ChronoUnit.HOURS), "RTGS"));
            }
            case "PASS_THROUGH" -> {
                FakeCustomer sender = getRandomCustomer(customers, primary);
                FakeCustomer receiver = getRandomCustomer(customers, primary);
                breachTxns.add(createCustomTxn(sender, primary,
                        "80000.00", now.minus(2, ChronoUnit.HOURS), "WIRE"));
                breachTxns.add(createCustomTxn(primary, receiver,
                        "79950.00", now.minus(1, ChronoUnit.HOURS), "WIRE"));
            }
            case "FUNNEL" -> {
                for (int i = 0; i < 4; i++) {
                    FakeCustomer sender = getRandomCustomer(customers, primary);
                    breachTxns.add(createCustomTxn(sender, primary,
                            "9500.00", now.minus(i * 30L, ChronoUnit.MINUTES), "ONLINE"));
                }
            }
            case "LARGE_TRANSACTION" ->
                    breachTxns.add(createCustomTxn(primary, getRandomCustomer(customers, primary),
                            "750000.00", now.minus(2, ChronoUnit.DAYS), "WIRE"));
            case "ROUND_AMOUNT" ->
                    breachTxns.add(createCustomTxn(primary, getRandomCustomer(customers, primary),
                            "50000.00", now.minus(5, ChronoUnit.DAYS), "ONLINE"));
        }
        return breachTxns;
    }

    private String[] createCustomTxn(FakeCustomer orig, FakeCustomer ben,
                                     String amount, OffsetDateTime timestamp, String type) {
        return new String[]{
                nextRef("TXN-BRCH-"),                    // ✅ unique ref
                orig.getAccountNumber(), orig.getCustomerName(), "OUR_BANK", orig.getCountryOfResidence(),
                ben.getAccountNumber(),  ben.getCustomerName(),  "OUR_BANK", ben.getCountryOfResidence(),
                amount, "USD", type, "ONLINE",
                timestamp.format(ISO_FORMATTER),
                "Suspicious Activity", "CLEAN"
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
        String transactionRef = nextRef("TXN"); // ✅ unique ref

        FakeCustomer primaryCustomer = customers.get(random.nextInt(customers.size()));

        String originatorAccountNo, originatorName, originatorBankCode, originatorCountry;
        String beneficiaryAccountNo, beneficiaryName, beneficiaryBankCode, beneficiaryCountry;

        boolean isInternalTransfer = random.nextInt(100) < 60;

        if (isInternalTransfer) {
            FakeCustomer secondary = getRandomCustomer(customers, primaryCustomer);
            originatorAccountNo  = primaryCustomer.getAccountNumber();
            originatorName       = primaryCustomer.getCustomerName();
            originatorCountry    = primaryCustomer.getCountryOfResidence();
            originatorBankCode   = "OUR_BANK";
            beneficiaryAccountNo = secondary.getAccountNumber();
            beneficiaryName      = secondary.getCustomerName();
            beneficiaryCountry   = secondary.getCountryOfResidence();
            beneficiaryBankCode  = "OUR_BANK";
        } else {
            boolean isOutgoing    = random.nextBoolean();
            String externalAccount = "EXT" + faker.number().digits(10);
            String externalName    = faker.company().name().replace(",", "");
            String externalCountry = LOW_RISK_COUNTRIES[random.nextInt(LOW_RISK_COUNTRIES.length)];
            String externalBank    = faker.finance().bic();

            if (isOutgoing) {
                originatorAccountNo  = primaryCustomer.getAccountNumber();
                originatorName       = primaryCustomer.getCustomerName();
                originatorCountry    = primaryCustomer.getCountryOfResidence();
                originatorBankCode   = "OUR_BANK";
                beneficiaryAccountNo = externalAccount;
                beneficiaryName      = externalName;
                beneficiaryCountry   = externalCountry;
                beneficiaryBankCode  = externalBank;
            } else {
                originatorAccountNo  = externalAccount;
                originatorName       = externalName;
                originatorCountry    = externalCountry;
                originatorBankCode   = externalBank;
                beneficiaryAccountNo = primaryCustomer.getAccountNumber();
                beneficiaryName      = primaryCustomer.getCustomerName();
                beneficiaryCountry   = primaryCustomer.getCountryOfResidence();
                beneficiaryBankCode  = "OUR_BANK";
            }
        }

        int currRoll = random.nextInt(100);
        String currencyCode = currRoll < 60 ? "INR" : currRoll < 85 ? "USD"
                                                      : CURRENCIES[random.nextInt(CURRENCIES.length)];

        double amountBase = 100 + (8000 * random.nextDouble());
        String transactionType = TXN_TYPES[random.nextInt(TXN_TYPES.length)];
        String channel         = CHANNELS[random.nextInt(CHANNELS.length)];

        if (index % 31 == 0)           { amountBase = 9900 + (99 * random.nextDouble()); channel = "CASH"; }
        if (random.nextInt(100) < 8)   { amountBase = 10000 + (25000 * random.nextDouble()); }
        if (random.nextInt(100) < 5)   {
            int[] multipliers = {1000, 5000};
            amountBase = multipliers[random.nextInt(multipliers.length)] * (1 + random.nextInt(5));
        }
        if (!isInternalTransfer && index % 43 == 0) {
            if (originatorBankCode.equals("OUR_BANK"))
                beneficiaryCountry = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
            else
                originatorCountry  = BLACKLIST_COUNTRIES[random.nextInt(BLACKLIST_COUNTRIES.length)];
        }
        if (channel.equals("UPI")) transactionType = "WIRE";
        if (channel.equals("ATM")) transactionType = "CASH";

        long totalSeconds  = ChronoUnit.SECONDS.between(START_DATE, END_DATE);
        long randomSeconds = (long) (random.nextDouble() * totalSeconds);
        OffsetDateTime timestamp = START_DATE.plusSeconds(randomSeconds);

        String referenceNote = "";
        if      (index % 89  == 0) referenceNote = "Loan payment";
        else if (index % 103 == 0) referenceNote = "Consulting fees";
        else if (random.nextBoolean()) referenceNote = "TXN REF " + faker.number().digits(4);

        String amount = BigDecimal.valueOf(amountBase).setScale(2, RoundingMode.HALF_UP).toString();

        return new String[]{
                transactionRef,
                originatorAccountNo, originatorName, originatorBankCode, originatorCountry,
                beneficiaryAccountNo, beneficiaryName, beneficiaryBankCode, beneficiaryCountry,
                amount, currencyCode, transactionType, channel,
                timestamp.format(ISO_FORMATTER), referenceNote, "CLEAN"
        };
    }
}
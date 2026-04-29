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
public class DormantTransactionCsvGenerator {

    private final Faker faker = new Faker(new Locale("en-IND"));
    private final Random random = new Random();

    private static final String HEADER = "transactionRef,originatorAccountNo,originatorName,originatorBankCode,originatorCountry,beneficiaryAccountNo,beneficiaryName,beneficiaryBankCode,beneficiaryCountry,amount,currencyCode,transactionType,channel,transactionTimestamp,referenceNote,status\n";

    public byte[] generate(int noiseCount, List<FakeCustomer> customers) {
        List<String[]> allTransactions = new ArrayList<>();


        for (int i = 1; i <= noiseCount; i++) {
            String[] row = buildNoiseRow(i, customers);
            if (!row[1].equals("BRK-DORMANT-01") && !row[5].equals("BRK-DORMANT-01")) {
                allTransactions.add(row);
            }
        }

        allTransactions.addAll(injectDormantBreach(customers));

        StringBuilder csvBuilder = new StringBuilder();
        csvBuilder.append(HEADER);
        for (String[] row : allTransactions) {
            csvBuilder.append(String.join(",", row)).append("\n");
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }


    private List<String[]> injectDormantBreach(List<FakeCustomer> customers) {
        List<String[]> breachTxns = new ArrayList<>();

        FakeCustomer target = customers.stream()
                .filter(c -> c.getAccountNumber().equals("BRK-DORMANT-01"))
                .findFirst()
                .orElse(customers.get(0));

        OffsetDateTime now = OffsetDateTime.now();


        for (int i = 0; i < 3; i++) {
            FakeCustomer otherParty = getRandomCustomer(customers, target);
            OffsetDateTime timestamp = now.minus(i * 4, ChronoUnit.HOURS);

            String amount = "20000.00";

            breachTxns.add(createCustomTxn(target, otherParty, amount, timestamp, "WIRE", "ONLINE"));
        }

        return breachTxns;
    }

    private String[] createCustomTxn(FakeCustomer orig, FakeCustomer ben, String amount, OffsetDateTime timestamp, String type, String channel) {
        String ref = "TXN-REACTIVE-" + System.currentTimeMillis() + random.nextInt(999);
        return new String[]{
                ref, orig.getAccountNumber(), orig.getCustomerName(), "OUR_BANK", "IND",
                ben.getAccountNumber(), ben.getCustomerName(), "OUR_BANK", "IND",
                amount, "INR", type, channel, timestamp.toString(), "Reactivation Activity", "CLEAN"
        };
    }

    private FakeCustomer getRandomCustomer(List<FakeCustomer> customers, FakeCustomer exclude) {
        FakeCustomer c = customers.get(random.nextInt(customers.size()));
        while (exclude != null && c.getAccountNumber().equals(exclude.getAccountNumber())) {
            c = customers.get(random.nextInt(customers.size()));
        }
        return c;
    }

    private String[] buildNoiseRow(int index, List<FakeCustomer> customers) {
        String transactionRef = "TXN-NSE-" + System.currentTimeMillis() + index;
        FakeCustomer c1 = customers.get(random.nextInt(customers.size()));
        FakeCustomer c2 = getRandomCustomer(customers, c1);

        return new String[]{
                transactionRef, c1.getAccountNumber(), c1.getCustomerName(), "OUR_BANK", "IND",
                c2.getAccountNumber(), c2.getCustomerName(), "OUR_BANK", "IND",
                "150.00", "INR", "TRANSFER", "MOBILE", OffsetDateTime.now().minus(random.nextInt(10), ChronoUnit.DAYS).toString(), "Noise", "CLEAN"
        };
    }
}
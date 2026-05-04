package com.app.aml.faker;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@RestController
@RequestMapping("/api/v1/dev/faker")
// @Profile("dev") // Uncomment this when you are done testing!
@RequiredArgsConstructor
public class FakerController {

    private final CustomerCsvGenerator customerCsvGenerator;
    private final TransactionCsvGenerator transactionCsvGenerator;
    private final CustomerProfileRepository customerProfileRepository;

    @GetMapping(value = "/customers", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> generateCustomers(@RequestParam(defaultValue = "100") int count) {
        log.info("Generating Faker CSV for {} customers", count);

        List<FakeCustomer> fakeCustomers = customerCsvGenerator.generateCustomerList(count);
        byte[] csvBytes = customerCsvGenerator.generateCsvBytes(fakeCustomers);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"faker_customers_" + count + ".csv\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> generateTransactions(
            @RequestParam(defaultValue = "500") int count,
            @RequestParam(defaultValue = "true") boolean withAccounts) {

        log.info("Generating Faker CSV for {} transactions. withAccounts={}", count, withAccounts);
        List<FakeCustomer> accountPool;

        if (withAccounts) {
            List<CustomerProfile> dbProfiles = customerProfileRepository.findAll();
            if (!dbProfiles.isEmpty()) {
                accountPool = dbProfiles.stream()
                        .map(p -> FakeCustomer.builder()
                                .accountNumber(p.getAccountNumber())
                                .customerName(p.getCustomerName()) // Adjust to match your entity getter
                                .countryOfResidence(p.getCountryOfResidence() != null ? p.getCountryOfResidence() : "IND")
                                .build())
                        .collect(Collectors.toList());
            } else {
                accountPool = customerCsvGenerator.generateCustomerList(20);
            }
        } else {
            accountPool = customerCsvGenerator.generateCustomerList(20);
        }

        byte[] csvBytes = transactionCsvGenerator.generate(count, accountPool);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"faker_transactions_" + count + ".csv\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

     //http://localhost:8080/api/v1/dev/faker/dataset?customerCount=100&txnCount=1000

    @GetMapping(value = "/dataset", produces = "application/zip")
    public ResponseEntity<byte[]> generateCohesiveDataset(
            @RequestParam(defaultValue = "100") int customerCount,
            @RequestParam(defaultValue = "1000") int txnCount) throws IOException {

        log.info("Generating Cohesive ZIP Dataset: {} customers, {} transactions", customerCount, txnCount);

        // 1. Generate the shared memory pool
        List<FakeCustomer> sharedCustomers = customerCsvGenerator.generateCustomerList(customerCount);

        // 2. Generate both CSV byte arrays from the EXACT same pool
        byte[] customerCsvBytes = customerCsvGenerator.generateCsvBytes(sharedCustomers);
        byte[] transactionCsvBytes = transactionCsvGenerator.generate(txnCount, sharedCustomers);

        // 3. Zip them together
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            // Add Customers CSV
            ZipEntry customerEntry = new ZipEntry("faker_customers.csv");
            zos.putNextEntry(customerEntry);
            zos.write(customerCsvBytes);
            zos.closeEntry();

            // Add Transactions CSV
            ZipEntry txnEntry = new ZipEntry("faker_transactions.csv");
            zos.putNextEntry(txnEntry);
            zos.write(transactionCsvBytes);
            zos.closeEntry();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"aml_dataset_" + System.currentTimeMillis() + ".zip\"");
        headers.setContentType(MediaType.valueOf("application/zip"));

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }
}
package com.app.aml.faker;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;
import jakarta.servlet.http.HttpServletResponse;
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
    public void generateTransactions(
            @RequestParam(defaultValue = "500") int count,
            @RequestParam(defaultValue = "true") boolean withAccounts,
            HttpServletResponse response) throws IOException {

        log.info("Generating {} transactions. withAccounts={}", count, withAccounts);

        List<FakeCustomer> accountPool = resolveAccountPool(withAccounts);

        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"faker_transactions_" + count + ".csv\"");

        // ✅ Stream directly to response — no byte[] in memory regardless of count
        transactionCsvGenerator.streamGenerate(count, accountPool, response.getOutputStream());
    }
    /**
     * NEW: Generates both CSVs perfectly synced together and downloads as a ZIP.
     * http://localhost:8080/api/v1/dev/faker/dataset?customerCount=100&txnCount=1000
     */

    @GetMapping(value = "/dataset", produces = "application/zip")
    public void generateCohesiveDataset(
            @RequestParam(defaultValue = "100") int customerCount,
            @RequestParam(defaultValue = "1000") int txnCount,
            HttpServletResponse response) throws IOException {

        log.info("Generating ZIP Dataset: {} customers, {} transactions", customerCount, txnCount);

        List<FakeCustomer> sharedCustomers = customerCsvGenerator.generateCustomerList(customerCount);
        byte[] customerCsvBytes = customerCsvGenerator.generateCsvBytes(sharedCustomers);

        response.setContentType("application/zip");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"aml_dataset_" + System.currentTimeMillis() + ".zip\"");

        try (ZipOutputStream zos = new ZipOutputStream(response.getOutputStream())) {
            // Customers (small — fine as byte[])
            zos.putNextEntry(new ZipEntry("faker_customers.csv"));
            zos.write(customerCsvBytes);
            zos.closeEntry();

            // Transactions — stream directly into zip
            zos.putNextEntry(new ZipEntry("faker_transactions.csv"));
            transactionCsvGenerator.streamGenerate(txnCount, sharedCustomers, zos);
            zos.closeEntry();
        }
    }

    // Extract helper to avoid duplication
    private List<FakeCustomer> resolveAccountPool(boolean withAccounts) {
        if (withAccounts) {
            List<CustomerProfile> dbProfiles = customerProfileRepository.findAll();
            if (!dbProfiles.isEmpty()) {
                return dbProfiles.stream()
                        .map(p -> FakeCustomer.builder()
                                .accountNumber(p.getAccountNumber())
                                .customerName(p.getCustomerName())
                                .countryOfResidence(p.getCountryOfResidence() != null
                                        ? p.getCountryOfResidence() : "IND")
                                .build())
                        .collect(Collectors.toList());
            }
        }
        return customerCsvGenerator.generateCustomerList(20);
    }


}
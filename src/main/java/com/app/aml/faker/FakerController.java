package com.app.aml.faker;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.repository.CustomerProfileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        byte[] csvBytes = customerCsvGenerator.generate(count);

        HttpHeaders headers = new HttpHeaders();
        // Notice the escaped quotes around the filename - this helps browsers force the save dialog
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"faker_customers_" + count + ".csv\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }


    //hit on postman exactly this request
    //http://localhost:8080/api/v1/dev/faker/transactions?count=500&withAccounts=false
    @GetMapping(value = "/transactions", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> generateTransactions(
            @RequestParam(defaultValue = "500") int count,
            @RequestParam(defaultValue = "true") boolean withAccounts) {

        log.info("Generating Faker CSV for {} transactions. withAccounts={}", count, withAccounts);
        List<String> accountPool;

        if (withAccounts) {
            accountPool = customerProfileRepository.findAll()
                    .stream()
                    .map(CustomerProfile::getAccountNumber)
                    .collect(Collectors.toList());

            if (accountPool.isEmpty()) {
                accountPool = generateDummyAccountPool();
            }
        } else {
            accountPool = generateDummyAccountPool();
        }

        byte[] csvBytes = transactionCsvGenerator.generate(count, accountPool);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"faker_transactions_" + count + ".csv\"");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
    }

    private List<String> generateDummyAccountPool() {
        return IntStream.range(1, 20)
                .mapToObj(i -> "DUMMY_ACCT_" + i)
                .collect(Collectors.toList());
    }
}
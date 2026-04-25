package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class CustomerValidationSkipListener implements SkipListener<CustomerProfileCsvDto, CustomerProfile> {

    private static final int MAX_ERRORS_TO_COLLECT = 1000;
    private static final int SYSTEM_KEY = -1;

    @Getter
    private final Map<Integer, List<String>> validationErrors = new ConcurrentHashMap<>();
    private final AtomicInteger errorCount = new AtomicInteger(0);

    @Override
    public void onSkipInProcess(CustomerProfileCsvDto item, Throwable t) {
        log.error("Customer Validation Skip [Row {}]: {}", item.getLineNumber(), t.getMessage());
        if (t instanceof ValidationException ex) {
            handleError(ex.getRow(), "Col: " + ex.getColumn() + " -> " + ex.getMessage());
        }
    }

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException parseEx) {
            log.error("Customer Read Skip [Line {}]: {}", parseEx.getLineNumber(), parseEx.getMessage());
            handleError(parseEx.getLineNumber(), "Malformed CSV structure");
        }
    }

    @Override
    public void onSkipInWrite(CustomerProfile item, Throwable t) {
        log.error("Customer Database Write Skip [Acc: {}]: {}", item.getAccountNumber(), t.getMessage());
    }

    private void handleError(int lineNumber, String message) {
        int current = errorCount.getAndIncrement();
        if (current < MAX_ERRORS_TO_COLLECT) {
            validationErrors
                    .computeIfAbsent(lineNumber, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(message);
        } else if (current == MAX_ERRORS_TO_COLLECT) {
            validationErrors
                    .computeIfAbsent(SYSTEM_KEY, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add("SYSTEM WARNING: Error limit reached. Truncating report.");
        }
    }

    public void clearErrors() {
        validationErrors.clear();
        errorCount.set(0);
    }
}
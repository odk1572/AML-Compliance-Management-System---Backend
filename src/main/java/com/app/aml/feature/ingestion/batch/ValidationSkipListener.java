package com.app.aml.feature.ingestion.batch;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import lombok.Getter;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ValidationSkipListener implements SkipListener<CustomerProfileCsvDto, CustomerProfile> {

    // Thread-safe collection for storing errors
    private static final int MAX_ERRORS_TO_COLLECT = 1000;
    @Getter
    private final Map<Integer, List<String>> validationErrors = new ConcurrentHashMap<>();
    private final AtomicInteger errorCount = new AtomicInteger(0);

    @Override
    public void onSkipInProcess(CustomerProfileCsvDto item, Throwable t) {
        if (t instanceof ValidationException ex) {
            if (errorCount.incrementAndGet() <= MAX_ERRORS_TO_COLLECT) {
                validationErrors.computeIfAbsent(ex.getRow(), k -> new CopyOnWriteArrayList<>())
                        .add("Col: " + ex.getColumn() + " -> " + ex.getMessage());
            } else if (errorCount.get() == MAX_ERRORS_TO_COLLECT + 1) {
                // Add a final truncation message
                validationErrors.computeIfAbsent(-1, k -> new CopyOnWriteArrayList<>())
                        .add("SYSTEM WARNING: Over " + MAX_ERRORS_TO_COLLECT + " errors found. Truncating error report.");
            } else if (t instanceof org.springframework.batch.item.file.FlatFileParseException parseEx) {
                validationErrors.computeIfAbsent(parseEx.getLineNumber(), k -> new CopyOnWriteArrayList<>())
                        .add("Col: N/A -> Malformed CSV structure or illegal characters.");
            }
        }
    }

    public void clearErrors() {
        validationErrors.clear();
    }
}
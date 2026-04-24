package com.app.aml.feature.ingestion.batch;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import lombok.Getter;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ValidationSkipListener implements SkipListener<CustomerProfileCsvDto, CustomerProfile> {

    private static final int MAX_ERRORS_TO_COLLECT = 1000;
    private static final int SYSTEM_KEY = -1;

    @Getter
    private final Map<Integer, List<String>> validationErrors = new ConcurrentHashMap<>();
    private final AtomicInteger errorCount = new AtomicInteger(0);

    @Override
    public void onSkipInProcess(CustomerProfileCsvDto item, Throwable t) {
        if (t instanceof ValidationException ex) {
            handleValidationError(ex);
        }
    }

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof org.springframework.batch.item.file.FlatFileParseException parseEx) {
            handleParseError(parseEx);
        }
    }

    private void handleValidationError(ValidationException ex) {
        int current = errorCount.getAndIncrement();

        if (current < MAX_ERRORS_TO_COLLECT) {
            validationErrors
                    .computeIfAbsent(ex.getRow(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add("Col: " + ex.getColumn() + " -> " + ex.getMessage());

        } else if (current == MAX_ERRORS_TO_COLLECT) {
            validationErrors
                    .computeIfAbsent(SYSTEM_KEY, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add("SYSTEM WARNING: More than " + MAX_ERRORS_TO_COLLECT + " errors. Truncated.");
        }
    }

    private void handleParseError(org.springframework.batch.item.file.FlatFileParseException ex) {
        int current = errorCount.getAndIncrement();

        if (current < MAX_ERRORS_TO_COLLECT) {
            validationErrors
                    .computeIfAbsent(ex.getLineNumber(), k -> Collections.synchronizedList(new ArrayList<>()))
                    .add("Malformed CSV or invalid structure");

        } else if (current == MAX_ERRORS_TO_COLLECT) {
            validationErrors
                    .computeIfAbsent(SYSTEM_KEY, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add("SYSTEM WARNING: More than " + MAX_ERRORS_TO_COLLECT + " errors. Truncated.");
        }
    }

    public void clearErrors() {
        validationErrors.clear();
        errorCount.set(0);
    }
}
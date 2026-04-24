package com.app.aml.feature.ingestion.batch.transaction;

import com.app.aml.feature.ingestion.batch.ValidationException;
import com.app.aml.feature.ingestion.entity.Transaction;
import lombok.Getter;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class TransactionValidationSkipListener implements SkipListener<TransactionCsvDto, Transaction> {

    private static final int MAX_ERRORS_TO_COLLECT = 1000;
    private static final int SYSTEM_KEY = -1;

    @Getter
    private final Map<Integer, List<String>> validationErrors = new ConcurrentHashMap<>();
    private final AtomicInteger errorCount = new AtomicInteger(0);

    @Override
    public void onSkipInProcess(TransactionCsvDto item, Throwable t) {
        if (t instanceof ValidationException ex) {
            handleError(ex.getRow(), "Col: " + ex.getColumn() + " -> " + ex.getMessage());
        }
    }

    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException parseEx) {
            handleError(parseEx.getLineNumber(), "Malformed CSV structure or invalid row format");
        }
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
                    .add("SYSTEM WARNING: More than " + MAX_ERRORS_TO_COLLECT + " errors found. Truncating report.");
        }
    }

    public void clearErrors() {
        validationErrors.clear();
        errorCount.set(0);
    }
}
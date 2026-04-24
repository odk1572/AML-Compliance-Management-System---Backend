package com.app.aml.feature.ingestion.batch.customer;

import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.notification.service.interfaces.MailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerIngestionJobCompletionListener implements JobExecutionListener {

    private final TransactionBatchRepository batchRepository;
    private final CustomerValidationSkipListener skipListener;
    private final ObjectMapper objectMapper;
    private final MailService mailService; // Assumes your mail service logic exists

    @Override
    public void beforeJob(JobExecution jobExecution) {
        skipListener.clearErrors();
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        // 1. Extract both parameters safely
        String batchIdStr = jobExecution.getJobParameters().getString("batchId");
        String filePath = jobExecution.getJobParameters().getString("filePath");

        if (batchIdStr == null) return;

        // 2. Parse the correct variable to UUID
        UUID batchId = UUID.fromString(batchIdStr);
        TransactionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch not found: " + batchId));

        Map<Integer, java.util.List<String>> errors = skipListener.getValidationErrors();

        if (!errors.isEmpty()) {
            // FAILED STATE - The ingestion step was bypassed.
            try {
                String failureJson = objectMapper.writeValueAsString(errors);
                batch.setFailureDetails(failureJson);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse errors to JSON", e);
            }
            batch.setBatchStatus(BatchStatus.FAILED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);

            // Stop Spring batch job execution gracefully
            jobExecution.setExitStatus(ExitStatus.FAILED);

            // Trigger failure email (Ensure mailService is injected via constructor)
            mailService.sendEmail("admin@yourdomain.com", "Batch Failed: " + batch.getFileName(), "Errors found. Please check system.");
        } else {
            // SUCCESS STATE
            batch.setBatchStatus(com.app.aml.domain.enums.BatchStatus.PROCESSED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);
        }

        // 3. Clean up the local temporary file to prevent disk space starvation
        if (filePath != null) {
            try {
                java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(filePath));
                log.info("Successfully deleted temporary batch file: {}", filePath);
            } catch (java.io.IOException e) {
                log.error("Failed to delete local temp file: " + filePath, e);
            }
        }
    }
}
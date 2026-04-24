package com.app.aml.feature.ingestion.batch.transaction;

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionJobCompletionListener implements JobExecutionListener {

    private final TransactionBatchRepository batchRepository;
    private final TransactionValidationSkipListener skipListener;
    private final ObjectMapper objectMapper;
    private final MailService mailService;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Vital: Clear out errors from previous jobs that used this singleton listener
        skipListener.clearErrors();
    }

    @Override
    @Transactional
    public void afterJob(JobExecution jobExecution) {
        String batchIdStr = jobExecution.getJobParameters().getString("batchId");
        String filePath = jobExecution.getJobParameters().getString("filePath");

        if (batchIdStr == null) return;

        UUID batchId = UUID.fromString(batchIdStr);
        TransactionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalStateException("Batch not found: " + batchId));

        Map<Integer, List<String>> errors = skipListener.getValidationErrors();

        if (!errors.isEmpty()) {
            // --- FAILURE PATH ---
            try {
                String failureJson = objectMapper.writeValueAsString(errors);
                batch.setFailureDetails(failureJson);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse Transaction batch errors to JSON", e);
            }
            batch.setBatchStatus(BatchStatus.FAILED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);

            jobExecution.setExitStatus(ExitStatus.FAILED);

            mailService.sendEmail(
                    "admin@yourdomain.com",
                    "Transaction Batch Failed: " + batch.getFileName(),
                    "Validation failed for " + errors.size() + " lines. Please check the system dashboard for the detailed JSON error report."
            );

            log.warn("Transaction Batch {} failed with {} validation errors.", batch.getBatchReference(), errors.size());

        } else {
            // --- SUCCESS PATH ---
            batch.setBatchStatus(BatchStatus.PROCESSED);
            batch.setProcessedAt(Instant.now());
            batchRepository.save(batch);

            log.info("Transaction Batch {} successfully processed and ingested.", batch.getBatchReference());
        }

        // --- DISK CLEANUP ---
        if (filePath != null) {
            try {
                Files.deleteIfExists(Path.of(filePath));
                log.info("Deleted temporary transaction batch file: {}", filePath);
            } catch (java.io.IOException e) {
                log.error("Failed to delete local temp file: {}", filePath, e);
            }
        }
    }
}
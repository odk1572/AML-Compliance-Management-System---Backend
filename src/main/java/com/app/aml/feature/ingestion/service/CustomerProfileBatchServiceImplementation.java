package com.app.aml.feature.ingestion.service;

import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.domain.exceptions.ApplicationException;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.mapper.TransactionBatchMapper;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.feature.ingestion.service.CustomerProfileBatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerProfileBatchServiceImplementation implements CustomerProfileBatchService {

    private final TransactionBatchRepository batchRepository;
    private final TransactionBatchMapper batchMapper;
    private final JobLauncher jobLauncher;

    @Qualifier("customerProfileIngestionJob")
    private final Job customerProfileIngestionJob;


    @Override
    @Transactional
    public TransactionBatchResponseDto uploadAndTriggerBatch(MultipartFile file, UUID uploadedBy) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            // FIX: Using RuntimeException because ApplicationException is abstract
            throw new RuntimeException("Uploaded file cannot be empty");
        }

        try {
            String fileHash = calculateSha256(file.getInputStream());

            if (batchRepository.existsByFileHashSha256(fileHash)) {
                throw new RuntimeException("A file with this exact content has already been uploaded.");
            }

            Path tempFilePath = Files.createTempFile("aml_batch_", ".csv");
            file.transferTo(tempFilePath.toFile());

            TransactionBatch batch = new TransactionBatch();
            batch.setBatchReference("BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            batch.setUploadedBy(uploadedBy);
            batch.setFileName(file.getOriginalFilename());
            batch.setFileHashSha256(fileHash);
            batch.setFileSizeBytes(file.getSize());
            batch.setBatchStatus(BatchStatus.PENDING);

            TransactionBatch savedBatch = batchRepository.save(batch);

            triggerBatchJobAsync(savedBatch.getId(), tempFilePath.toAbsolutePath().toString());

            // FIX: Changed toResponseDto to match standard naming
            return batchMapper.toResponseDto(savedBatch);

        } catch (Exception e) {
            log.error("Failed to process batch upload", e);
            throw new RuntimeException("Failed to initialize batch upload: " + e.getMessage());
        }
    }

    @Async
    public void triggerBatchJobAsync(UUID batchId, String filePath) {
        log.info("Starting background batch job for batchId: {}", batchId);
        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addString("batchId", batchId.toString())
                    .addString("filePath", filePath)
                    // Adding timestamp guarantees unique Job Instances in Spring Batch
                    .addLong("executionTime", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(customerProfileIngestionJob, parameters);

        } catch (Exception e) {
            log.error("Spring Batch Job execution failed for batchId: {}", batchId, e);
            // Fallback status update if the job completely fails to launch
            batchRepository.findById(batchId).ifPresent(batch -> {
                batch.setBatchStatus(BatchStatus.FAILED);
                batch.setFailureDetails("{\"system_error\": \"Job failed to launch inside thread.\"}");
                batchRepository.save(batch);
            });
        }
    }

    // Helper method to hash the file
    private String calculateSha256(InputStream is) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int read;
        while ((read = is.read(buffer)) > 0) {
            digest.update(buffer, 0, read);
        }
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
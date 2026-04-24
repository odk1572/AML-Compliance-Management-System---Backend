package com.app.aml.feature.ingestion.service;

import com.app.aml.domain.enums.BatchFileType;
import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.domain.exceptions.BusinessRuleException;
import com.app.aml.domain.exceptions.DuplicateBatchException;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.mapper.TransactionBatchMapper;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
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
public class UniversalBatchIngestionService {

    private final TransactionBatchRepository batchRepository;
    private final TransactionBatchMapper batchMapper;
    private final JobLauncher jobLauncher;

    // Inject BOTH of your Spring Batch Jobs here
    @Qualifier("customerProfileIngestionJob")
    private final Job customerJob;

    @Qualifier("transactionIngestionJob")
    private final Job transactionJob;

    @Transactional
    public TransactionBatchResponseDto uploadAndRouteBatch(MultipartFile file, UUID uploadedBy, BatchFileType fileType) {

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new BusinessRuleException("Uploaded file cannot be empty");
        }

        try {
            String fileHash = calculateSha256(file.getInputStream());

            if (batchRepository.existsByFileHashSha256(fileHash)) {
                throw new DuplicateBatchException(fileHash);
            }

            Path tempFilePath = Files.createTempFile("aml_" + fileType.name().toLowerCase() + "_", ".csv");
            file.transferTo(tempFilePath.toFile());

            TransactionBatch batch = new TransactionBatch();
            // Dynamically generate the prefix (e.g., CUST-1234 or TXN-1234)
            String prefix = fileType == BatchFileType.CUSTOMER_PROFILE ? "CUST-" : "TXN-";
            batch.setBatchReference(prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            batch.setUploadedBy(uploadedBy);
            batch.setFileName(file.getOriginalFilename());
            batch.setFileHashSha256(fileHash);
            batch.setFileSizeBytes(file.getSize());
            batch.setBatchStatus(BatchStatus.PENDING);

            TransactionBatch savedBatch = batchRepository.save(batch);

            // Pass the fileType to the Async runner so it knows which job to trigger
            triggerTargetedBatchJobAsync(savedBatch.getId(), tempFilePath.toAbsolutePath().toString(), fileType);

            return batchMapper.toResponseDto(savedBatch);

        } catch (Exception e) {
            log.error("Failed to process {} batch upload", fileType, e);
            throw new BusinessRuleException("Failed to initialize batch upload: " + e.getMessage() +" BATCH_INIT_ERROR");
        }
    }

    @Async
    public void triggerTargetedBatchJobAsync(UUID batchId, String filePath, BatchFileType fileType) {
        log.info("Starting background {} batch job for batchId: {}", fileType, batchId);
        try {
            JobParameters parameters = new JobParametersBuilder()
                    .addString("batchId", batchId.toString())
                    .addString("filePath", filePath)
                    .addLong("executionTime", System.currentTimeMillis())
                    .toJobParameters();

            // The Routing Logic
            Job targetJob = switch (fileType) {
                case CUSTOMER_PROFILE -> customerJob;
                case TRANSACTION -> transactionJob;
            };

            jobLauncher.run(targetJob, parameters);

        } catch (Exception e) {
            log.error("Spring Batch Job execution failed for batchId: {}", batchId, e);
            batchRepository.findById(batchId).ifPresent(batch -> {
                batch.setBatchStatus(BatchStatus.FAILED);
                batch.setFailureDetails("{\"system_error\": \"Job failed to launch.\"}");
                batchRepository.save(batch);
            });
        }
    }

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
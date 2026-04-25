package com.app.aml.feature.ingestion.service;

import com.app.aml.domain.enums.BatchFileType;
import com.app.aml.domain.enums.BatchStatus;
import com.app.aml.domain.exceptions.BusinessRuleException;
import com.app.aml.domain.exceptions.DuplicateBatchException;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.mapper.TransactionBatchMapper;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.multitenency.TenantContext; // Required to fetch the tenant ID
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor // Safe to use this again!
public class UniversalBatchIngestionService {

    private final TransactionBatchRepository batchRepository;
    private final TransactionBatchMapper batchMapper;
    private final AsyncBatchLauncher asyncBatchLauncher; // Inject our new Async Launcher

    // REMOVED @Transactional to prevent the Spring Batch crash!
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
            String prefix = fileType == BatchFileType.CUSTOMER_PROFILE ? "CUST-" : "TXN-";
            batch.setBatchReference(prefix + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            batch.setUploadedBy(uploadedBy);
            batch.setFileName(file.getOriginalFilename());
            batch.setFileHashSha256(fileHash);
            batch.setFileSizeBytes(file.getSize());
            batch.setBatchStatus(BatchStatus.PENDING);

            // Database save happens safely here (JPA is transactional by default)
            TransactionBatch savedBatch = batchRepository.save(batch);

            // Fetch the current Tenant ID from the context

            // Fire the background job using the new Async component
            // 1. Capture the schema name while still on the request thread
            String currentTenantId = TenantContext.getTenantId();
            String currentSchemaName = TenantContext.getSchemaName(); // <-- Capture this

// 2. Pass BOTH the ID and the Schema to the launcher
            asyncBatchLauncher.triggerTargetedBatchJobAsync(
                    savedBatch.getId(),
                    tempFilePath.toAbsolutePath().toString(),
                    fileType,
                    currentTenantId,
                    currentSchemaName // <-- ADD THIS 5th ARGUMENT
            );

            return batchMapper.toResponseDto(savedBatch);

        } catch (Exception e) {
            log.error("Failed to process {} batch upload", fileType, e);
            throw new BusinessRuleException("Failed to initialize batch upload: " + e.getMessage() +" BATCH_INIT_ERROR");
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
package com.app.aml.feature.ingestion.service;

import com.app.aml.enums.BatchFileType;
import com.app.aml.enums.BatchStatus;
import com.app.aml.exceptions.BusinessRuleException;
import com.app.aml.exceptions.DuplicateBatchException;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import com.app.aml.feature.ingestion.mapper.TransactionBatchMapper;
import com.app.aml.feature.ingestion.repository.TransactionBatchRepository;
import com.app.aml.multitenency.TenantContext; // Required to fetch the tenant ID
import com.app.aml.annotation.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    private final AsyncBatchLauncher asyncBatchLauncher;

    @AuditAction(category = "DATA_INGESTION", action = "UPLOAD_BATCH", entityType = "BATCH")
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

            TransactionBatch savedBatch = batchRepository.save(batch);

            String currentTenantId = TenantContext.getTenantId();
            String currentSchemaName = TenantContext.getSchemaName();
            asyncBatchLauncher.triggerTargetedBatchJobAsync(
                    savedBatch.getId(),
                    tempFilePath.toAbsolutePath().toString(),
                    fileType,
                    currentTenantId,
                    currentSchemaName
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

    @AuditAction(category = "DATA_ACCESS", action = "VIEW_BATCH_STATUS", entityType = "BATCH")
    public TransactionBatchResponseDto getBatchStatus(UUID batchId) {
        TransactionBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new BusinessRuleException("Batch not found with ID: " + batchId));

        return batchMapper.toResponseDto(batch);
    }


    @AuditAction(category = "DATA_ACCESS", action = "LIST_BATCHES", entityType = "BATCH")
    public Page<TransactionBatchResponseDto> getAllBatches(BatchFileType fileType, Pageable pageable) {
        Page<TransactionBatch> batchPage = batchRepository.findAll(pageable);

        return batchPage.map(batchMapper::toResponseDto);
    }
}
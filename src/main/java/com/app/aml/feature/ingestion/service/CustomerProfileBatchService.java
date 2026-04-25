package com.app.aml.feature.ingestion.service;


import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CustomerProfileBatchService {

    TransactionBatchResponseDto uploadAndTriggerBatch(MultipartFile file, UUID uploadedBy);
}
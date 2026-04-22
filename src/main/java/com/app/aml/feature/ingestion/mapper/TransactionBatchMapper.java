package com.app.aml.feature.ingestion.mapper;


import com.app.aml.feature.ingestion.dto.transactionBatch.request.CreateTransactionBatchRequestDto;
import com.app.aml.feature.ingestion.dto.transactionBatch.response.TransactionBatchResponseDto;
import com.app.aml.feature.ingestion.dto.transactionBatch.request.UpdateTransactionBatchProgressDto;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionBatchMapper {


    TransactionBatchResponseDto toResponseDto(TransactionBatch entity);

    List<TransactionBatchResponseDto> toResponseDtoList(List<TransactionBatch> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "totalRecords", ignore = true)
    @Mapping(target = "batchStatus", ignore = true) // Entity defaults to PENDING
    @Mapping(target = "failureDetails", ignore = true)
    @Mapping(target = "springBatchJobId", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    TransactionBatch toEntity(CreateTransactionBatchRequestDto dto);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "batchReference", ignore = true)
    @Mapping(target = "uploadedBy", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "fileHashSha256", ignore = true)
    @Mapping(target = "fileSizeBytes", ignore = true)
    @Mapping(target = "cloudinaryPublicId", ignore = true)
    @Mapping(target = "cloudinarySecureUrl", ignore = true)
    void updateProgress(UpdateTransactionBatchProgressDto dto, @MappingTarget TransactionBatch entity);
}
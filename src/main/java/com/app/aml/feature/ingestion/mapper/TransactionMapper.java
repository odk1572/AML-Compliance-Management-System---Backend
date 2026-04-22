package com.app.aml.feature.ingestion.mapper;

import com.app.aml.feature.ingestion.dto.transaction.request.CreateTransactionRequestDto;
import com.app.aml.feature.ingestion.dto.transaction.response.TransactionResponseDto;
import com.app.aml.feature.ingestion.dto.transaction.request.UpdateTransactionStatusRequestDto;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.ingestion.entity.TransactionBatch;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {

    @Mapping(target = "batchId", source = "batch.id")
    @Mapping(target = "customerId", source = "customer.id")
    TransactionResponseDto toResponseDto(Transaction entity);

    List<TransactionResponseDto> toResponseDtoList(List<Transaction> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "batch", source = "batchId")
    @Mapping(target = "customer", source = "customerId")
    Transaction toEntity(CreateTransactionRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "batch", ignore = true)
    @Mapping(target = "customer", ignore = true)
    void updateStatus(UpdateTransactionStatusRequestDto dto, @MappingTarget Transaction entity);

    default TransactionBatch mapBatchIdToBatch(UUID batchId) {
        if (batchId == null) {
            return null;
        }
        TransactionBatch batch = new TransactionBatch();
        batch.setId(batchId);
        return batch;
    }

    default CustomerProfile mapCustomerIdToCustomer(UUID customerId) {
        if (customerId == null) {
            return null;
        }
        CustomerProfile customer = new CustomerProfile();
        customer.setId(customerId);
        return customer;
    }
}
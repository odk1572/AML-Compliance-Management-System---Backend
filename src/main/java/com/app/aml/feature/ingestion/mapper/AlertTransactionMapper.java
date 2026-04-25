package com.app.aml.feature.ingestion.mapper;


import com.app.aml.feature.ingestion.dto.alertTransaction.AlertTransactionRequestDto;
import com.app.aml.feature.ingestion.dto.alertTransaction.AlertTransactionResponseDto;
import com.app.aml.feature.ingestion.entity.AlertTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertTransactionMapper {

    /**
     * Maps Entity to Response DTO (Flattening)
     * We use the dot notation to reach into the nested Transaction object.
     */
    @Mapping(source = "transaction.id", target = "transactionId")
    @Mapping(source = "transaction.transactionRef", target = "transactionRef")
    @Mapping(source = "transaction.amount", target = "amount")
    @Mapping(source = "transaction.currencyCode", target = "currencyCode")
    @Mapping(source = "transaction.transactionTimestamp", target = "transactionTimestamp")
    @Mapping(source = "transaction.originatorAccountNo", target = "originatorAccountNo")
    @Mapping(source = "transaction.beneficiaryAccountNo", target = "beneficiaryAccountNo")
    AlertTransactionResponseDto toResponseDto(AlertTransaction entity);

    /**
     * Maps a List of Entities to a List of Response DTOs
     */
    List<AlertTransactionResponseDto> toResponseDtoList(List<AlertTransaction> entities);

    /**
     * Maps Request DTO to Entity
     * Note: alert and transaction entities should be fetched from DB via service,
     * but we map the metadata (InvolvementRole) here.
     */
    @Mapping(target = "alert", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    AlertTransaction toEntity(AlertTransactionRequestDto dto);
}
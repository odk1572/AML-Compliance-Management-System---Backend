package com.app.aml.feature.alert.mapper;

import com.app.aml.feature.alert.dto.alertTransaction.AlertTransactionRequestDto;
import com.app.aml.feature.alert.dto.alertTransaction.AlertTransactionResponseDto;
import com.app.aml.feature.alert.entity.AlertTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertTransactionMapper {

    @Mapping(source = "transaction.id", target = "transactionId")
    @Mapping(source = "transaction.transactionRef", target = "transactionRef")
    @Mapping(source = "transaction.amount", target = "amount")
    @Mapping(source = "transaction.currencyCode", target = "currencyCode") // Reverted to currencyCode
    @Mapping(source = "transaction.transactionTimestamp", target = "transactionTimestamp")
    @Mapping(source = "transaction.originatorAccountNo", target = "originatorAccountNo")
    @Mapping(source = "transaction.beneficiaryAccountNo", target = "beneficiaryAccountNo")
        // Removed role and transactionType as they belong to the Summary DTO
    AlertTransactionResponseDto toResponseDto(AlertTransaction entity);

    List<AlertTransactionResponseDto> toResponseDtoList(List<AlertTransaction> entities);

    @Mapping(target = "alert", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    AlertTransaction toEntity(AlertTransactionRequestDto dto);
}
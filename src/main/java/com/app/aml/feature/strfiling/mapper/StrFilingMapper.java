package com.app.aml.feature.strfiling.mapper;

import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingRequestDto;
import com.app.aml.feature.strfiling.dto.strFiling.StrFilingResponseDto;
import com.app.aml.feature.strfiling.entity.StrFiling;
import com.app.aml.feature.strfiling.entity.StrFilingTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StrFilingMapper {

    @Mapping(target = "caseId", source = "caseRecord.id")
    @Mapping(target = "customer", source = "customer")
    @Mapping(target = "transactions", source = "strTransactions")
    StrFilingResponseDto toResponseDto(StrFiling entity);

    List<StrFilingResponseDto> toResponseDtoList(List<StrFiling> entities);

    StrFilingResponseDto.StrCustomerDto mapCustomer(CustomerProfile customer);

    default StrFilingResponseDto.LinkedTransactionDto mapTransaction(StrFilingTransaction strFilingTransaction) {
        if (strFilingTransaction == null || strFilingTransaction.getTransaction() == null) {
            return null;
        }
        Transaction txn = strFilingTransaction.getTransaction();

        return StrFilingResponseDto.LinkedTransactionDto.builder()
                .id(txn.getId())
                .transactionReference(txn.getTransactionRef())
                .amount(txn.getAmount())
                .currency(txn.getCurrencyCode())
                .transactionTimestamp(txn.getTransactionTimestamp())
                .transactionType(txn.getTransactionType() != null ? txn.getTransactionType().name() : null)
                .originatorAccount(txn.getOriginatorAccountNo())
                .beneficiaryAccount(txn.getBeneficiaryAccountNo())
                .build();
    }


    @Mapping(target = "caseRecord.id", source = "caseId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "filingReference", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "strTransactions", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "typologyTriggered", ignore = true)
    StrFiling toEntity(StrFilingRequestDto dto, UUID caseId);
}
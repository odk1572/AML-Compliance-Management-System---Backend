package com.app.aml.feature.strfiling.mapper;


import com.app.aml.feature.ingestion.entity.Transaction;
import com.app.aml.feature.strfiling.dto.strFilingTransaction.CreateStrFilingTransactionRequestDto;
import com.app.aml.feature.strfiling.dto.strFilingTransaction.StrFilingTransactionResponseDto;
import com.app.aml.feature.strfiling.entity.StrFiling;
import com.app.aml.feature.strfiling.entity.StrFilingTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface StrFilingTransactionMapper {

    @Mapping(target = "strFilingId", source = "strFiling.id")
    @Mapping(target = "transactionId", source = "transaction.id")
    StrFilingTransactionResponseDto toResponseDto(StrFilingTransaction entity);

    List<StrFilingTransactionResponseDto> toResponseDtoList(List<StrFilingTransaction> entities);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "strFiling", source = "strFilingId")
    @Mapping(target = "transaction", source = "transactionId")
    StrFilingTransaction toEntity(CreateStrFilingTransactionRequestDto dto);

   default StrFiling mapIdToStrFiling(UUID strFilingId) {
        if (strFilingId == null) return null;
        StrFiling filing = new StrFiling();
        filing.setId(strFilingId);
        return filing;
    }

    default Transaction mapIdToTransaction(UUID transactionId) {
        if (transactionId == null) return null;
        Transaction txn = new Transaction();
        txn.setId(transactionId);
        return txn;
    }
}
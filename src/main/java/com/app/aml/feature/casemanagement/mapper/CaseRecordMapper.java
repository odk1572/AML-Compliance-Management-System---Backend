package com.app.aml.feature.casemanagement.mapper;

import com.app.aml.feature.casemanagement.dto.caseRecord.response.CaseResponseDto;
import com.app.aml.feature.casemanagement.dto.caseRecord.request.CloseCaseRequestDto;
import com.app.aml.feature.casemanagement.dto.caseRecord.request.CreateCaseRequestDto;
import com.app.aml.feature.casemanagement.dto.caseRecord.request.UpdateCaseRequestDto;
import com.app.aml.feature.casemanagement.entity.CaseRecord;
import com.app.aml.feature.casemanagement.entity.CaseTransaction;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import com.app.aml.feature.ingestion.entity.Transaction;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CaseRecordMapper {

    // --- MAIN MAPPINGS ---

    @Mapping(source = "customer", target = "customer")
    @Mapping(source = "caseTransactions", target = "transactions")
    CaseResponseDto toResponseDto(CaseRecord entity);

    List<CaseResponseDto> toResponseDtoList(List<CaseRecord> entities);

    // --- NESTED / RICH DATA MAPPERS ---

    // Maps the Customer entity to the lightweight DTO
    CaseResponseDto.CaseCustomerDto mapCustomer(CustomerProfile customer);

    // Unpacks the actual Transaction from the CaseTransaction join table
    default CaseResponseDto.LinkedTransactionDto mapTransaction(CaseTransaction caseTransaction) {
        if (caseTransaction == null || caseTransaction.getTransaction() == null) {
            return null;
        }
        Transaction txn = caseTransaction.getTransaction();

        return CaseResponseDto.LinkedTransactionDto.builder()
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

    // --- ENTITY CREATION & UPDATES ---

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "lastActivityAt", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "typologyTriggered", ignore = true)
    @Mapping(target = "customer", ignore = true) // Handled in Service layer logic
    @Mapping(target = "status", constant = "OPEN")
    CaseRecord toEntity(CreateCaseRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "caseReference", ignore = true)
    @Mapping(target = "openedAt", ignore = true)
    @Mapping(target = "ruleType", ignore = true)
    @Mapping(target = "typologyTriggered", ignore = true)
    @Mapping(target = "customer", ignore = true) // Do not overwrite customer on status/priority updates
    void updateEntityFromDto(UpdateCaseRequestDto dto, @MappingTarget CaseRecord entity);

    @Mapping(target = "status", constant = "CLOSED_NO_ACTION")
    void closeCase(CloseCaseRequestDto dto, @MappingTarget CaseRecord entity);
}
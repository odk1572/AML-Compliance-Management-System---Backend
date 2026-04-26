package com.app.aml.feature.ingestion.mapper;

import com.app.aml.feature.ingestion.dto.alert.response.AlertResponseDto;
import com.app.aml.feature.ingestion.dto.alert.request.CreateAlertRequestDto;
import com.app.aml.feature.ingestion.dto.alert.request.UpdateAlertStatusRequestDto;
import com.app.aml.feature.ingestion.entity.Alert;
import com.app.aml.feature.ingestion.entity.Transaction;
import org.mapstruct.*;

import java.util.List;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertMapper {

    @Mapping(target = "transactionId", source = "transaction.id")
    AlertResponseDto toResponseDto(Alert entity);

    List<AlertResponseDto> toResponseDtoList(List<Alert> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true) // Defaults to NEW in entity
    @Mapping(target = "transaction", source = "transactionId")
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    Alert toEntity(CreateAlertRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customerProfileId", ignore = true)
    @Mapping(target = "transaction", ignore = true)
    @Mapping(target = "tenantScenarioId", ignore = true)
    @Mapping(target = "globalScenarioId", ignore = true)
    @Mapping(target = "globalRuleId", ignore = true)
    @Mapping(target = "tenantRuleId", ignore = true)
    @Mapping(target = "alertReference", ignore = true)
    @Mapping(target = "severity", ignore = true)
    @Mapping(target = "typologyTriggered", ignore = true)
    @Mapping(target = "riskScore", ignore = true)
    @Mapping(target = "sysCreatedAt", ignore = true)
    @Mapping(target = "sysUpdatedAt", ignore = true)
    void updateStatus(UpdateAlertStatusRequestDto dto, @MappingTarget Alert entity);

    /**
     * Maps a UUID to a JPA Transaction proxy.
     */
    default Transaction mapTransactionIdToTransaction(UUID transactionId) {
        if (transactionId == null) {
            return null;
        }
        Transaction transaction = new Transaction();
        transaction.setId(transactionId);
        return transaction;
    }
}
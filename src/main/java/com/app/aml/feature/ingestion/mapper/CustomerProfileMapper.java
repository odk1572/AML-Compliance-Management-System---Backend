package com.app.aml.feature.ingestion.mapper;

import com.app.aml.feature.ingestion.dto.customerProfile.request.CreateCustomerProfileRequestDto;
import com.app.aml.feature.ingestion.dto.customerProfile.response.CustomerProfileResponseDto;
import com.app.aml.feature.ingestion.dto.customerProfile.request.UpdateCustomerProfileRequestDto;
import com.app.aml.feature.ingestion.entity.CustomerProfile;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerProfileMapper {

    CustomerProfileResponseDto toResponseDto(CustomerProfile entity);

    List<CustomerProfileResponseDto> toResponseDtoList(List<CustomerProfile> entities);

    @Mapping(target = "id", ignore = true)
    CustomerProfile toEntity(CreateCustomerProfileRequestDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "accountNumber", ignore = true)
    @Mapping(target = "accountOpenedOn", ignore = true)
    void updateEntityFromDto(UpdateCustomerProfileRequestDto dto, @MappingTarget CustomerProfile entity);
}
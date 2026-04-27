package com.app.aml.feature.notification.mapper;


import com.app.aml.feature.notification.dto.InPlatformNotificationResponseDto;
import com.app.aml.feature.notification.dto.UpdateNotificationStatusDto;
import com.app.aml.feature.notification.entity.InPlatformNotification;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InPlatformNotificationMapper {

    InPlatformNotificationResponseDto toResponseDto(InPlatformNotification entity);

    List<InPlatformNotificationResponseDto> toResponseDtoList(List<InPlatformNotification> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recipientId", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "body", ignore = true)
    void updateStatus(UpdateNotificationStatusDto dto, @MappingTarget InPlatformNotification entity);
}
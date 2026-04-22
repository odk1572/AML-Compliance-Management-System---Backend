package com.app.aml.feature.notification.mapper;


import com.app.aml.feature.notification.dto.InPlatformNotificationResponseDto;
import com.app.aml.feature.notification.dto.UpdateNotificationStatusDto;
import com.app.aml.feature.notification.entity.InPlatformNotification;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InPlatformNotificationMapper {

    /**
     * Maps Entity to Response DTO.
     * MapStruct automatically picks up sysCreatedAt from the parent AuditableEntity.
     */
    InPlatformNotificationResponseDto toResponseDto(InPlatformNotification entity);

    /**
     * Maps a list of Entities to a list of Response DTOs.
     * Useful for the Inbox/Bell notification list.
     */
    List<InPlatformNotificationResponseDto> toResponseDtoList(List<InPlatformNotification> entities);

    /**
     * Updates only the 'isRead' status of an existing notification.
     * Use this in the Service layer before calling markAsRead() logic.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "recipientId", ignore = true)
    @Mapping(target = "notificationType", ignore = true)
    @Mapping(target = "title", ignore = true)
    @Mapping(target = "body", ignore = true)
    void updateStatus(UpdateNotificationStatusDto dto, @MappingTarget InPlatformNotification entity);
}
package com.app.aml.feature.notification.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.notification.dto.BulkMarkAsReadRequestDto;
import com.app.aml.feature.notification.dto.InPlatformNotificationResponseDto;
import com.app.aml.feature.notification.service.interfaces.NotificationService;
import com.app.aml.annotation.AuditAction;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;


    @GetMapping
    @AuditAction(category = "NOTIFICATION", action = "VIEW_NOTIFICATIONS")
    public ResponseEntity<ApiResponse<List<InPlatformNotificationResponseDto>>> getMyNotifications(
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request) {

        List<InPlatformNotificationResponseDto> data = notificationService.getUserNotifications(userId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "User notifications retrieved successfully.",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request) {

        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Unread notification count retrieved.",
                request.getRequestURI(),
                count
        ));
    }

    @PatchMapping("/{id}/read")
    @AuditAction(category = "NOTIFICATION", action = "MARK_READ", entityType = "NOTIFICATION")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request) {

        notificationService.markAsRead(id, userId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Notification marked as read.",
                request.getRequestURI(),
                null
        ));
    }


    @PostMapping("/read-all")
    @AuditAction(category = "NOTIFICATION", action = "MARK_ALL_READ")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request) {

        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "All notifications marked as read.",
                request.getRequestURI(),
                null
        ));
    }


    @PostMapping("/bulk-read")
    @AuditAction(category = "NOTIFICATION", action = "BULK_MARK_READ")
    public ResponseEntity<ApiResponse<Void>> markSelectedAsRead(
            @RequestBody @Valid BulkMarkAsReadRequestDto dto,
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request) {

        notificationService.markSelectedAsRead(dto, userId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Selected notifications marked as read.",
                request.getRequestURI(),
                null
        ));
    }
}
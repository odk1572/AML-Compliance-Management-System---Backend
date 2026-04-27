package com.app.aml.feature.platformuser.controller;

import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.platformuser.dto.CreatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.dto.PlatformUserResponseDto;
import com.app.aml.feature.platformuser.dto.UpdatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.service.PlatformUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform-users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformUserController {

    private final PlatformUserService platformUserService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<PlatformUserResponseDto>> getMe(HttpServletRequest request) {
        String principal = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID currentUserId = UUID.fromString(principal);

        PlatformUserResponseDto response = platformUserService.getMe(currentUserId);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Profile retrieved successfully",
                request.getRequestURI(),
                response
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformUserResponseDto>> getPlatformUserById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        PlatformUserResponseDto response = platformUserService.getPlatformUserById(id);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Platform user retrieved successfully",
                request.getRequestURI(),
                response
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PlatformUserResponseDto>>> getAllPlatformUsers(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<PlatformUserResponseDto> response = platformUserService.getAllPlatformUsers(pageable);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Platform users retrieved successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PlatformUserResponseDto>> createPlatformUser(
            @Valid @RequestBody CreatePlatformUserRequestDto dto,
            HttpServletRequest request) {

        PlatformUserResponseDto response = platformUserService.createPlatformUser(dto);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                HttpStatus.CREATED,
                "Platform user created successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PlatformUserResponseDto>> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlatformUserRequestDto dto,
            HttpServletRequest request) {

        PlatformUserResponseDto response = platformUserService.updateProfile(id, dto);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Profile updated successfully",
                request.getRequestURI(),
                response
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePlatformUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        platformUserService.deletePlatformUser(id);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Platform user deleted successfully",
                request.getRequestURI(),
                null
        ));
    }

    @PatchMapping("/{id}/lock")
    public ResponseEntity<ApiResponse<PlatformUserResponseDto>> lockUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        PlatformUserResponseDto response = platformUserService.lockUser(id);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Platform user locked successfully",
                request.getRequestURI(),
                response
        ));
    }

    @PatchMapping("/{id}/unlock")
    public ResponseEntity<ApiResponse<PlatformUserResponseDto>> unlockUser(
            @PathVariable UUID id,
            HttpServletRequest request) {

        PlatformUserResponseDto response = platformUserService.unlockUser(id);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Platform user unlocked successfully",
                request.getRequestURI(),
                response
        ));
    }
}
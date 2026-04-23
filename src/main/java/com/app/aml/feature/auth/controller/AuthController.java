package com.app.aml.feature.auth.controller;


import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.auth.dto.*;
import com.app.aml.feature.auth.service.interfaces.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService  authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto loginRequest,
            HttpServletRequest request) {

        LoginResponseDto data = authService.login(loginRequest);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Authentication successful",
                request.getRequestURI(),
                data
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(
            @Valid @RequestBody TokenRefreshRequestDto refreshRequest,
            HttpServletRequest request) {

        LoginResponseDto data = authService.refreshToken(refreshRequest);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Token refreshed successfully",
                request.getRequestURI(),
                data
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            HttpServletRequest request) {

        authService.logout(authHeader);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Logout successful",
                request.getRequestURI(),
                null
        ));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDto changePasswordRequest,
            HttpServletRequest request) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString((String) auth.getPrincipal());

        @SuppressWarnings("unchecked")
        Map<String, String> details = (Map<String, String>) auth.getDetails();
        boolean isPlatform = details.get("tenantId") == null;

        authService.changePassword(changePasswordRequest, userId, isPlatform);

        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Password updated successfully",
                request.getRequestURI(),
                null
        ));
    }
}
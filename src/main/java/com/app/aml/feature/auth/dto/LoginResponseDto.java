package com.app.aml.feature.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    private String username;
    private String role;
    private UUID tenantId; // Null for Platform Users

    private boolean isFirstLogin;
}
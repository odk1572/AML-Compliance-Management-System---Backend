package com.app.aml.feature.auth.service.interfaces;

import com.app.aml.feature.auth.dto.ChangePasswordRequestDto;
import com.app.aml.feature.auth.dto.LoginRequestDto;
import com.app.aml.feature.auth.dto.LoginResponseDto;
import com.app.aml.feature.auth.dto.TokenRefreshRequestDto;

import java.util.UUID;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto dto);
    LoginResponseDto refreshToken(TokenRefreshRequestDto dto);
    void logout(String authHeader);
    void changePassword(ChangePasswordRequestDto dto, UUID userId, boolean isPlatform);
}

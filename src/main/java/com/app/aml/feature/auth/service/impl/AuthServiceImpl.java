package com.app.aml.feature.auth.service.impl;

import com.app.aml.feature.auth.dto.*;
import com.app.aml.feature.auth.entity.RefreshToken;
import com.app.aml.feature.auth.repository.RefreshTokenRepository;
import com.app.aml.feature.auth.service.interfaces.AuthService;
import com.app.aml.feature.platformuser.entity.PlatformUser;
import com.app.aml.feature.platformuser.repository.PlatformUserRepository;
import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.repository.TenantRepository;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;
import com.app.aml.multitenency.TenantContext;
import com.app.aml.security.entity.PlatformUserSession;
import com.app.aml.security.entity.UserSession;
import com.app.aml.security.jwt.JtiBlacklistService;
import com.app.aml.security.jwt.JwtTokenProvider;
import com.app.aml.security.repository.PlatformUserSessionRepository;
import com.app.aml.security.repository.UserSessionRepository;
import com.app.aml.security.userDetails.PlatformUserDetails;
import com.app.aml.security.userDetails.TenantUserDetails;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    @Lazy
    private final AuthenticationManager authenticationManager;


    private final JwtTokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PlatformUserSessionRepository platformSessionRepo;
    private final UserSessionRepository tenantSessionRepo;
    private final PlatformUserRepository platformUserRepository;
    private final TenantUserRepository tenantUserRepository;
    private final JtiBlacklistService jtiBlacklistService;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;

    @Value("${app.security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    public LoginResponseDto login(LoginRequestDto dto) {
        // 1. Set explicit tenant if provided, else clear for discovery
        if (dto.getTenantId() != null) {
            TenantContext.setTenantId(dto.getTenantId().toString());
        } else {
            TenantContext.clear();
        }

        // 2. Authenticate
        // This triggers UnifiedUserDetailsService which might perform "Discovery"
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
        );

        String userId;
        String role;
        boolean isFirstLogin;
        UUID finalTenantId = dto.getTenantId();
        String sessionSchema = null; // To track where the session table lives

        Object principal = authentication.getPrincipal();

        // 3. Process Platform User
        if (principal instanceof PlatformUserDetails platformUserDetails) {
            PlatformUser user = platformUserDetails.getPlatformUser();
            if (user.isLocked()) throw new RuntimeException("Account is locked");

            userId = user.getId().toString();
            role = user.getRole().name();
            isFirstLogin = user.isFirstLogin();

            user.recordSuccessfulLogin("unknown-ip");
            platformUserRepository.save(user);

            finalTenantId = null;
            sessionSchema = "common_schema"; // Sessions go to common_schema

            // 4. Process Tenant User
        } else if (principal instanceof TenantUserDetails tenantUserDetails) {
            TenantUser user = tenantUserDetails.getTenantUser();
            if (user.isLocked()) throw new RuntimeException("Account is locked");

            userId = user.getId().toString();
            role = user.getRole().name();
            isFirstLogin = user.isFirstLogin();

            user.setLastLoginAt(java.time.Instant.now());
            tenantUserRepository.save(user);

            // Retrieve the schema name (either from DTO or Discovery)
            sessionSchema = TenantContext.getTenantId();

            if (finalTenantId == null) {
                Tenant tenant = tenantRepository.findBySchemaName(sessionSchema)
                        .orElseThrow(() -> new RuntimeException("Tenant not found for schema: " ));
                finalTenantId = tenant.getId();
            }
        } else {
            throw new IllegalStateException("Unknown Principal type");
        }

        // 5. Generate Tokens
        String accessToken = tokenProvider.generateToken(userId, finalTenantId != null ? finalTenantId.toString() : null, role);
        String jti = tokenProvider.extractJti(accessToken);
        java.time.Instant expiry = tokenProvider.extractAllClaims(accessToken).getExpiration().toInstant();

        // 6. --- CRITICAL SESSION ROUTING ---
        // Instead of clearing blindly, we set the context to where the user_sessions table is.
        if (finalTenantId != null && sessionSchema != null) {
            TenantContext.setTenantId(sessionSchema); // Route to Bank Schema
        } else {
            TenantContext.clear(); // Route to common_schema/public
        }

        try {
            persistSession(UUID.fromString(userId), finalTenantId, jti, expiry);

            String rawRefreshToken = UUID.randomUUID().toString();
            saveRefreshToken(UUID.fromString(userId), finalTenantId, rawRefreshToken);

            return LoginResponseDto.builder()
                    .accessToken(accessToken)
                    .refreshToken(rawRefreshToken)
                    .username(dto.getEmail())
                    .role(role)
                    .tenantId(finalTenantId)
                    .isFirstLogin(isFirstLogin)
                    .build();
        } finally {
            // 7. ALWAYS clear at the very end
            TenantContext.clear();
        }
    }
    @Transactional
    public LoginResponseDto refreshToken(TokenRefreshRequestDto dto) {
        String hashedToken = hashToken(dto.getRefreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hashedToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.isInvalid()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired or revoked");
        }

        String userId = refreshToken.getUserId().toString();
        UUID tenantId = refreshToken.getTenantId();
        String role;

        if (tenantId == null) {
            role = platformUserRepository.findById(refreshToken.getUserId())
                    .orElseThrow().getRole().name();
        } else {
            role = tenantUserRepository.findById(refreshToken.getUserId())
                    .orElseThrow().getRole().name();
        }

        String newAccessToken = tokenProvider.generateToken(userId, tenantId != null ? tenantId.toString() : null, role);
        String jti = tokenProvider.extractJti(newAccessToken);
        Instant expiry = tokenProvider.extractAllClaims(newAccessToken).getExpiration().toInstant();

        persistSession(UUID.fromString(userId), tenantId, jti, expiry);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(dto.getRefreshToken())
                .role(role)
                .tenantId(tenantId)
                .build();
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return;
        String token = authHeader.substring(7);

        Claims claims = tokenProvider.extractAllClaims(token);
        String jti = claims.getId();
        String tenantId = claims.get("tenantId", String.class);

        jtiBlacklistService.blacklistToken(jti, tenantId);

        if (tenantId == null) {
            platformSessionRepo.revokeSessionByJti(jti, Instant.now());
        } else {
            tenantSessionRepo.revokeSessionByJti(jti, Instant.now());
        }
    }

    @Transactional
    public void changePassword(ChangePasswordRequestDto dto, UUID userId, boolean isPlatform) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (isPlatform) {
            PlatformUser user = platformUserRepository.findById(userId).orElseThrow();
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid old password");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
            user.setFirstLogin(false);
            platformUserRepository.save(user);
        } else {
            TenantUser user = tenantUserRepository.findById(userId).orElseThrow();
            if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
                throw new IllegalArgumentException("Invalid old password");
            }
            user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
            user.setFirstLogin(false);
            tenantUserRepository.save(user);
        }

        refreshTokenRepository.revokeAllByUserId(userId);
    }

    private void persistSession(UUID userId, UUID tenantId, String jti, Instant expiry) {
        if (tenantId == null) {
            PlatformUserSession session = PlatformUserSession.builder()
                    .userId(userId)
                    .jwtJti(jti)
                    .expiresAt(expiry)
                    .isRevoked(false)
                    .build();
            platformSessionRepo.save(session);
        } else {
            UserSession session = UserSession.builder()
                    .userId(userId)
                    .jwtJti(jti)
                    .expiresAt(expiry)
                    .isRevoked(false)
                    .build();
            tenantSessionRepo.save(session);
        }
    }

    private void saveRefreshToken(UUID userId, UUID tenantId, String rawToken) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tenantId(tenantId)
                .tokenHash(hashToken(rawToken))
                .expiryDate(Instant.now().plusMillis(refreshExpirationMs))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
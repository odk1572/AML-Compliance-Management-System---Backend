package com.app.aml.feature.platformuser.dto;

import com.app.aml.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformUserResponseDto {

    private UUID id;

    private String email;

    private String fullName;

    private Role role;

    private boolean locked;

    private Instant lockedAt;

    private Instant lastLoginAt;

    private String lastLoginIp;
}
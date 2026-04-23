package com.app.aml.feature.platformuser.service;

import com.app.aml.feature.platformuser.dto.CreatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.dto.PlatformUserResponseDto;
import com.app.aml.feature.platformuser.dto.UpdatePlatformUserRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PlatformUserService {

    PlatformUserResponseDto getMe(UUID userId);

    PlatformUserResponseDto getPlatformUserById(UUID id);

    Page<PlatformUserResponseDto> getAllPlatformUsers(Pageable pageable);

    PlatformUserResponseDto updateProfile(UUID id, UpdatePlatformUserRequestDto dto);

    PlatformUserResponseDto createPlatformUser(CreatePlatformUserRequestDto dto);

    void deletePlatformUser(UUID id);

    PlatformUserResponseDto lockUser(UUID id);

    PlatformUserResponseDto unlockUser(UUID id);
}
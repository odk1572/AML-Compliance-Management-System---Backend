package com.app.aml.feature.platformuser.service;

import com.app.aml.feature.platformuser.dto.CreatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.dto.PlatformUserResponseDto;
import com.app.aml.feature.platformuser.dto.UpdatePlatformUserRequestDto;
import com.app.aml.feature.platformuser.entity.PlatformUser;
import com.app.aml.feature.platformuser.mapper.PlatformUserMapper;
import com.app.aml.feature.platformuser.repository.PlatformUserRepository;
import com.app.aml.annotation.AuditAction;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PlatformUserServiceImpl implements PlatformUserService {

    private final PlatformUserRepository platformUserRepository;
    private final PlatformUserMapper platformUserMapper;
    private final PasswordEncoder passwordEncoder;

    private UUID getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof String) {
            return UUID.fromString((String) authentication.getPrincipal());
        }
        throw new AccessDeniedException("User not authenticated");
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_SELF_PROFILE", entityType = "PLATFORM_USER")
    public PlatformUserResponseDto getMe(UUID userId) {
        PlatformUser user = platformUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Platform user not found with ID: " + userId));

        return platformUserMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_PLATFORM_USER_DETAILS", entityType = "PLATFORM_USER")
    public PlatformUserResponseDto getPlatformUserById(UUID id) {
        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform user not found with ID: " + id));

        return platformUserMapper.toResponseDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "LIST_PLATFORM_USERS", entityType = "PLATFORM_USER")
    public Page<PlatformUserResponseDto> getAllPlatformUsers(Pageable pageable) {
        return platformUserRepository.findAll(pageable)
                .map(platformUserMapper::toResponseDto);
    }

    @Override
    @Transactional
    @AuditAction(category = "USER_MGMT", action = "UPDATE_PLATFORM_USER_PROFILE", entityType = "PLATFORM_USER")
    public PlatformUserResponseDto updateProfile(UUID id, UpdatePlatformUserRequestDto dto) {
        if (!id.equals(getCurrentUserId())) {
            throw new AccessDeniedException("You can only update your own profile");
        }

        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform user not found with ID: " + id));

        if (!user.getEmail().equalsIgnoreCase(dto.getEmail()) &&
                platformUserRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use by another account");
        }

        platformUserMapper.updateEntityFromDto(dto, user);

        PlatformUser updatedUser = platformUserRepository.save(user);
        return platformUserMapper.toResponseDto(updatedUser);
    }


    @Override
    @Transactional
    @AuditAction(category = "ADMIN", action = "CREATE_PLATFORM_USER", entityType = "PLATFORM_USER")
    public PlatformUserResponseDto createPlatformUser(CreatePlatformUserRequestDto dto) {
        if (platformUserRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already in use");
        }

        PlatformUser user = platformUserMapper.toEntity(dto);
        user.setId(UuidCreator.getTimeOrderedEpoch());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        PlatformUser savedUser = platformUserRepository.save(user);

        return platformUserMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional
    @AuditAction(category = "ADMIN", action = "DELETE_PLATFORM_USER", entityType = "PLATFORM_USER")
    public void deletePlatformUser(UUID id) {
        UUID currentUserId = getCurrentUserId();

        if (id.equals(currentUserId)) {
            throw new IllegalArgumentException("You cannot delete your own account. High-level Super Admin accounts must be managed by another administrator.");
        }

        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform user not found with ID: " + id));
        platformUserRepository.delete(user);
    }

    @Override
    @Transactional
    @AuditAction(category = "SECURITY", action = "LOCK_PLATFORM_USER", entityType = "PLATFORM_USER")
    public PlatformUserResponseDto lockUser(UUID id) {
        if (id.equals(getCurrentUserId())) {
            throw new IllegalArgumentException("You cannot lock your own account");
        }

        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform user not found with ID: " + id));

        user.lockAccount();

        PlatformUser savedUser = platformUserRepository.saveAndFlush(user);

        return platformUserMapper.toResponseDto(savedUser);
    }

    @Override
    @Transactional
    @AuditAction(category = "SECURITY", action = "UNLOCK_PLATFORM_USER", entityType = "PLATFORM_USER")
    public PlatformUserResponseDto unlockUser(UUID id) {
        if (id.equals(getCurrentUserId())) {
            throw new IllegalArgumentException("You cannot unlock your own account");
        }

        PlatformUser user = platformUserRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform user not found with ID: " + id));

        user.unlockAccount();
        PlatformUser savedUser = platformUserRepository.save(user);
        return platformUserMapper.toResponseDto(savedUser);
    }
}
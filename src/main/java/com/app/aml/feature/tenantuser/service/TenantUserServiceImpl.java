package com.app.aml.feature.tenantuser.service;

import com.app.aml.feature.tenantuser.dto.CreateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.dto.TenantUserResponseDto;
import com.app.aml.feature.tenantuser.dto.UpdateTenantUserRequestDto;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.mapper.TenantUserMapper;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;
import com.app.aml.feature.casemanagement.repository.CaseRecordRepository;
import com.app.aml.security.rbac.Role;
import com.app.aml.shared.audit.service.AuditLogService;
import com.app.aml.feature.notification.service.interfaces.MailService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantUserServiceImpl implements TenantUserService {

    private final TenantUserRepository userRepo;
    private final CaseRecordRepository caseRepo;
    private final MailService mailService;
    private final AuditLogService auditLog;
    private final PasswordEncoder encoder;
    private final TenantUserMapper mapper;

    @Override
    @Transactional
    public TenantUserResponseDto createComplianceOfficer(CreateTenantUserRequestDto dto) {
        if (userRepo.existsByEmailAndSysIsDeletedFalse(dto.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + dto.getEmail());
        }

        String tempPassword = generateTempPassword();

        TenantUser user = mapper.toEntity(dto);
        user.setPasswordHash(encoder.encode(tempPassword));
        user.setFirstLogin(true);

        TenantUser savedUser = userRepo.save(user);
        TenantUserResponseDto responseDto = mapper.toResponseDto(savedUser);

        mailService.sendEmail(
                savedUser.getEmail(),
                "Welcome to AML Platform",
                "Your account has been created. Your temporary password is: " + tempPassword
        );

        auditLog.logTenant(
                null,
                "USER_MGMT",
                "CREATE_USER",
                "TENANT_USER",
                savedUser.getId(),
                null,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    public void deactivateUser(UUID id) {
        TenantUser user = userRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        TenantUserResponseDto prevState = mapper.toResponseDto(user);

        user.setSysIsDeleted(true);
        user.setSysDeletedAt(Instant.now());
        user.setLocked(true);
        userRepo.save(user);

        caseRepo.unassignOpenCasesForUser(id);

        auditLog.logTenant(
                null,
                "USER_MGMT",
                "DEACTIVATE_USER",
                "TENANT_USER",
                user.getId(),
                prevState,
                java.util.Map.of("status", "DEACTIVATED")
        );
    }

    @Override
    @Transactional
    public void resetPassword(UUID id) {
        TenantUser user = userRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        String tempPassword = generateTempPassword();
        user.setPasswordHash(encoder.encode(tempPassword));
        user.setFirstLogin(true);
        user.resetFailedAttempts();

        userRepo.save(user);

        mailService.sendEmail(
                user.getEmail(),
                "AML Platform - Password Reset",
                "Your password has been reset. Your new temporary password is: " + tempPassword
        );

        auditLog.logTenant(
                null,
                "USER_MGMT",
                "RESET_PASSWORD",
                "TENANT_USER",
                user.getId(),
                null,
                java.util.Map.of("action", "PASSWORD_RESET")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantUserResponseDto> listUsers(Role role, Pageable pageable) {
        if (role != null) {
            return userRepo.findByRoleAndSysIsDeletedFalse(role, pageable)
                    .map(mapper::toResponseDto);
        }
        return userRepo.findAllBySysIsDeletedFalse(pageable)
                .map(mapper::toResponseDto);
    }

    @Override
    @Transactional
    public TenantUserResponseDto updateUser(UUID id, UpdateTenantUserRequestDto dto) {
        TenantUser user = userRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        TenantUserResponseDto prevState = mapper.toResponseDto(user);

        if (dto.getIsLocked() != null) {
            if (user.isLocked() && !dto.getIsLocked()) {
                user.resetFailedAttempts();
            } else if (!user.isLocked() && dto.getIsLocked()) {
                user.lock();
            }
        }
        mapper.updateEntityFromDto(dto, user);

        TenantUser updatedUser = userRepo.save(user);
        TenantUserResponseDto response = mapper.toResponseDto(updatedUser);

        auditLog.logTenant(
                null,
                "USER_MGMT",
                "UPDATE_USER",
                "TENANT_USER",
                id,
                prevState,
                response
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantUserResponseDto getUserById(UUID id) {
        return userRepo.findByIdAndSysIsDeletedFalse(id)
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + id));
    }

    @Override
    @Transactional
    public void reactivateUser(UUID id) {
        TenantUser user = userRepo.findDeletedById(id)
                .orElseThrow(() -> new EntityNotFoundException("Deleted user not found with ID: " + id));

        user.setSysIsDeleted(false);
        user.setSysDeletedAt(null);

        user.resetFailedAttempts();
        user.setLocked(false);

        userRepo.save(user);

        auditLog.logTenant(
                null,
                "USER_MGMT",
                "REACTIVATE_USER",
                "TENANT_USER",
                user.getId(),
                null,
                java.util.Map.of("status", "ACTIVE")
        );
    }

    @Override
    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        TenantUser user = userRepo.findByIdAndSysIsDeletedFalse(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid current password");
        }

        user.setPasswordHash(encoder.encode(newPassword));
        user.setFirstLogin(false);
        user.resetFailedAttempts();

        userRepo.save(user);

        auditLog.logTenant(
                userId,
                "USER_MGMT",
                "CHANGE_PASSWORD",
                "TENANT_USER",
                userId,
                null,
                java.util.Map.of("isFirstLoginComplete", true)
        );
    }

    @Override
    @Transactional
    public void unlockUser(UUID id) {
        TenantUser user = userRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.isLocked()) {
            return;
        }

        user.resetFailedAttempts();
        userRepo.save(user);

        auditLog.logTenant(
                null,
                "USER_MGMT",
                "UNLOCK_USER",
                "TENANT_USER",
                id,
                null,
                java.util.Map.of("action", "MANUAL_UNLOCK")
        );
    }

    private String generateTempPassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
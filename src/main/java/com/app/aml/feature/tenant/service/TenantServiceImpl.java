package com.app.aml.feature.tenant.service;

import com.app.aml.domain.enums.TenantStatus;
import com.app.aml.multitenency.TenantSchemaResolver;
import jakarta.persistence.EntityManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.PlatformTransactionManager;
import com.app.aml.feature.notification.event.TenantCreatedEvent;
import com.app.aml.feature.notification.service.interfaces.MailService;
import com.app.aml.feature.tenant.dto.request.CreateTenantRequestDto;
import com.app.aml.feature.tenant.dto.request.UpdateTenantRequestDto;
import com.app.aml.feature.tenant.dto.response.TenantResponseDto;
import com.app.aml.feature.tenant.entity.Tenant;
import com.app.aml.feature.tenant.mapper.TenantMapper;
import com.app.aml.feature.tenant.repository.TenantRepository;
import com.app.aml.feature.tenantuser.entity.TenantUser;
import com.app.aml.feature.tenantuser.repository.TenantUserRepository;
import com.app.aml.multitenency.TenantContext;
import com.app.aml.multitenency.TenantSchemaDeactivator;
import com.app.aml.multitenency.TenantSchemaProvisioner;
import com.app.aml.security.rbac.Role;
import com.app.aml.shared.audit.service.AuditLogService;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantSchemaResolver schemaResolver;
    private final TenantUserRepository tenantUserRepository;
    private final TenantMapper tenantMapper;
    private final TenantSchemaProvisioner provisioner;
    private final TenantSchemaDeactivator deactivator;
    private final PasswordEncoder encoder;
    private final MailService mailService;
    private final AuditLogService auditLog;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final EntityManager entityManager; // <--- Add this!

    @Override
    // REMOVE @Transactional from here so we can control the boundaries manually
    public TenantResponseDto createTenant(CreateTenantRequestDto requestDto) {
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);

        // --- CRITICAL FIX 1: Force a brand new transaction/connection ---
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 1. PHASE 1: Platform Level (Common Schema)
        Tenant savedTenant = txTemplate.execute(status -> {
            if (tenantRepository.existsByTenantCode(requestDto.getTenantCode())) {
                throw new EntityExistsException("Tenant code already exists");
            }
            if (tenantRepository.existsBySchemaName(requestDto.getSchemaName())) {
                throw new EntityExistsException("Schema name already exists");
            }

            Tenant tenant = tenantMapper.toEntity(requestDto);
            tenant.setId(UuidCreator.getTimeOrderedEpoch());
            tenant.setStatus(TenantStatus.ACTIVE);
            return tenantRepository.save(tenant);
        });

        // 2. PHASE 2: Infrastructure (Flyway)
        provisioner.provision(savedTenant.getSchemaName());

        String rawPassword = generateSecurePassword();

        // 3. PHASE 3: Tenant Level (Specific Schema)
        TenantContext.setTenantId(savedTenant.getSchemaName());
        try {
            txTemplate.executeWithoutResult(status -> {

                // --- CRITICAL FIX 2: Clear Hibernate's cache so it "forgets" the common_schema ---
                entityManager.clear();

                TenantUser bankAdmin = TenantUser.builder()
                        .id(UuidCreator.getTimeOrderedEpoch()) // Ensure ID is explicitly generated
                        .employeeId("ADMIN-" + savedTenant.getTenantCode())
                        .email(savedTenant.getContactEmail())
                        .fullName(savedTenant.getInstitutionName() + " Admin")
                        .passwordHash(encoder.encode(rawPassword))
                        .role(Role.BANK_ADMIN)
                        .build();

                // Force flush to ensure the INSERT happens while the connection is routed
                tenantUserRepository.saveAndFlush(bankAdmin);
            });
        } finally {
            TenantContext.clear();
        }

        // 4. PHASE 4: Post-Process
        sendOnboardingEmail(savedTenant, rawPassword);

        eventPublisher.publishEvent(new TenantCreatedEvent(
                this, savedTenant.getId(), savedTenant.getInstitutionName(),
                savedTenant.getSchemaName(), savedTenant.getContactEmail(), savedTenant.getTenantCode()
        ));

        TenantResponseDto responseDto = tenantMapper.toResponseDto(savedTenant);
        auditLog.logPlatform(null, "TENANT_MGMT", "CREATE_TENANT", "TENANT", savedTenant.getId(), null, responseDto);

        return responseDto;
    }

    private void sendOnboardingEmail(Tenant tenant, String password) {
        String credSubject = "Your Temporary AML Platform Credentials";
        String credBody = String.format("Hello,\n\nYour temporary password for Bank Code %s is: %s\n\n" +
                        "Please log in and change your password immediately upon first access.",
                tenant.getTenantCode(), password);
        mailService.sendEmail(tenant.getContactEmail(), credSubject, credBody);
    }

    @Override
    @Transactional
    public TenantResponseDto updateTenant(UUID id, UpdateTenantRequestDto requestDto) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with ID: " + id));

        // Snapshot previous state for the audit log
        TenantResponseDto prevState = tenantMapper.toResponseDto(tenant);

        tenantMapper.updateEntityFromDto(requestDto, tenant);
        Tenant updatedTenant = tenantRepository.save(tenant);

        TenantResponseDto nextState = tenantMapper.toResponseDto(updatedTenant);

        // Detailed Platform Audit Log with Prev & Next states
        auditLog.logPlatform(null, "TENANT_MGMT", "UPDATE_TENANT", "TENANT", updatedTenant.getId(), prevState, nextState);

        return nextState;
    }


    @Override
    @Transactional
    public void deactivateTenant(UUID id) {
        // 1. Find the tenant in common_schema
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with ID: " + id));

        TenantResponseDto prevState = tenantMapper.toResponseDto(tenant);

        // 2. Update status in common_schema
        tenant.setStatus(TenantStatus.SUSPENDED);
        Tenant updatedTenant = tenantRepository.save(tenant);

        // 3. Switch Context for Routing, but pass the UUID for Logic
        // This tells the DataSource: "Go to hdfc_bank_01_schema"
        TenantContext.setTenantId(tenant.getSchemaName());
        try {
            // This tells the Deactivator: "Here is the UUID of the bank you are cleaning up"
            // Passing tenant.getId().toString() fixes the IllegalArgumentException
            deactivator.deactivate(tenant.getId().toString());
        } finally {
            TenantContext.clear();
        }

        // 4. Evict the Resolver Cache
        schemaResolver.evict(id.toString());

        // 5. Audit Logging
        TenantResponseDto nextState = tenantMapper.toResponseDto(updatedTenant);
        auditLog.logPlatform(null, "TENANT_MGMT", "DEACTIVATE_TENANT", "TENANT",
                tenant.getId(), prevState, nextState);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TenantResponseDto> listTenants(Pageable pageable) {
        return tenantRepository.findAll(pageable)
                .map(tenantMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    public TenantResponseDto getTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with ID: " + id));

        return tenantMapper.toResponseDto(tenant);
    }

    /**
     * Used by the Auth/Routing layer to resolve a tenant's details by their unique code.
     */
    @Override
    @Transactional(readOnly = true)
    public TenantResponseDto getTenantByCode(String tenantCode) {
        Tenant tenant = tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with code: " + tenantCode));
        return tenantMapper.toResponseDto(tenant);
    }

    /**
     * Reverses the deactivation process.
     */
    @Override
    @Transactional
    public TenantResponseDto reactivateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with ID: " + id));

        TenantResponseDto prevState = tenantMapper.toResponseDto(tenant);

        tenant.setStatus(TenantStatus.ACTIVE);
        Tenant updatedTenant = tenantRepository.save(tenant);

        // You might need a corresponding provisioner.reactivate(tenant.getSchemaName())
        // if your deactivator actually locks the DB schema.

        TenantResponseDto nextState = tenantMapper.toResponseDto(updatedTenant);
        auditLog.logPlatform(null, "TENANT_MGMT", "REACTIVATE_TENANT", "TENANT", id, prevState, nextState);

        return nextState;
    }

    /**
     * Quick check for frontend validation during the onboarding wizard.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isTenantCodeAvailable(String tenantCode) {
        return !tenantRepository.existsByTenantCode(tenantCode);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSchemaNameAvailable(String schemaName) {
        return !tenantRepository.existsBySchemaName(schemaName);
    }


    @Override
// 1. REMOVE @Transactional from the method itself
    public void resetBankAdminCredentials(UUID id) {
        // 2. We use a TransactionTemplate to control boundaries manually
        TransactionTemplate txTemplate = new TransactionTemplate(transactionManager);
        txTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // PHASE 1: Get Tenant Details (Common Schema)
        // Runs in a standard connection
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found with ID: " + id));

        String newPassword = generateSecurePassword();

        // PHASE 2: Reset Admin (Tenant Schema)
        // We use the Schema Name to ensure the Resolver handles it instantly
        TenantContext.setTenantId(tenant.getSchemaName());
        try {
            txTemplate.executeWithoutResult(status -> {
                // entityManager.clear() is a safety net to wipe the common_schema cache
                entityManager.clear();

                TenantUser admin = tenantUserRepository.findByEmailIgnoreCase(tenant.getContactEmail())
                        .orElseThrow(() -> new EntityNotFoundException("Bank Admin user not found"));

                admin.setPasswordHash(encoder.encode(newPassword));
                admin.setFirstLogin(true);

                tenantUserRepository.saveAndFlush(admin);
            });
        } finally {
            TenantContext.clear();
        }

        // PHASE 3: Communications
        mailService.sendEmail(
                tenant.getContactEmail(),
                "AML Platform - Credentials Reset",
                "Your temporary password is: " + newPassword
        );

        auditLog.logPlatform(null, "TENANT_MGMT", "RESET_ADMIN_CREDENTIALS", "TENANT", id, null, "Credentials regenerated");
    }

    private String generateSecurePassword() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[12];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
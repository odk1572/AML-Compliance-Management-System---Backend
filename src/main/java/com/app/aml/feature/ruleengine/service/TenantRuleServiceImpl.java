package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.tenantRule.request.UpdateTenantRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRule.response.TenantRuleResponseDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.CreateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.request.UpdateTenantRuleThresholdRequestDto;
import com.app.aml.feature.ruleengine.dto.tenantRuleThreshold.response.TenantRuleThresholdResponseDto;
import com.app.aml.feature.ruleengine.entity.GlobalRuleCondition;
import com.app.aml.feature.ruleengine.entity.TenantRule;
import com.app.aml.feature.ruleengine.entity.TenantRuleThreshold;
import com.app.aml.feature.ruleengine.mapper.TenantRuleMapper;
import com.app.aml.feature.ruleengine.mapper.TenantRuleThresholdMapper;
import com.app.aml.feature.ruleengine.repository.GlobalRuleConditionRepository;
import com.app.aml.feature.ruleengine.repository.TenantRuleRepository;
import com.app.aml.feature.ruleengine.repository.TenantRuleThresholdRepository;
import com.app.aml.annotation.AuditAction;
import com.app.aml.audit.service.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantRuleServiceImpl implements TenantRuleService {

    private final TenantRuleRepository tenantRuleRepo;
    private final TenantRuleThresholdRepository tenantRuleThresholdRepo;
    private final TenantRuleMapper tenantRuleMapper;
    private final TenantRuleThresholdMapper tenantRuleThresholdMapper;
    private final AuditLogService auditLogService;
    private final GlobalRuleConditionRepository globalConditionRepo;

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_TENANT_RULE", entityType = "TENANT_RULE")
    public TenantRuleResponseDto getRuleById(UUID ruleId) {
        TenantRule rule = tenantRuleRepo.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule not found with ID: " + ruleId));
        return tenantRuleMapper.toResponseDto(rule);
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPDATE_TENANT_RULE", entityType = "TENANT_RULE")
    public TenantRuleResponseDto updateRule(UUID ruleId, UpdateTenantRuleRequestDto dto) {
        TenantRule rule = tenantRuleRepo.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule not found with ID: " + ruleId));

        TenantRuleResponseDto oldState = tenantRuleMapper.toResponseDto(rule);
        tenantRuleMapper.updateEntityFromDto(dto, rule);
        TenantRule savedRule = tenantRuleRepo.save(rule);
        TenantRuleResponseDto response = tenantRuleMapper.toResponseDto(savedRule);

        auditLogService.log(
                null,
                "TENANT_RULE",
                "UPDATE",
                "TenantRule",
                rule.getId(),
                oldState,
                response
        );

        return response;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "TOGGLE_RULE_STATUS", entityType = "TENANT_RULE")
    public TenantRuleResponseDto toggleRule(UUID ruleId, boolean isActive) {
        TenantRule rule = tenantRuleRepo.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule not found with ID: " + ruleId));

        boolean oldActive = rule.isActive();
        rule.setActive(isActive);
        TenantRule savedRule = tenantRuleRepo.save(rule);
        TenantRuleResponseDto response = tenantRuleMapper.toResponseDto(savedRule);

        auditLogService.log(
                null,
                "TENANT_RULE",
                "TOGGLE",
                "TenantRule",
                rule.getId(),
                "{\"isActive\":" + oldActive + "}",
                "{\"isActive\":" + isActive + "}"
        );

        return response;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "CREATE_THRESHOLD_OVERRIDE", entityType = "RULE_THRESHOLD")
    public TenantRuleThresholdResponseDto createThresholdOverride(CreateTenantRuleThresholdRequestDto dto) {

        TenantRule tenantRule = tenantRuleRepo.findByRuleCode(dto.getTenantRuleCode())
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule not found with code: " + dto.getTenantRuleCode()));

        GlobalRuleCondition globalCondition = globalConditionRepo.findByConditionCode(dto.getGlobalConditionCode())
                .orElseThrow(() -> new EntityNotFoundException("Global Condition not found with code: " + dto.getGlobalConditionCode()));

        TenantRuleThreshold threshold = tenantRuleThresholdMapper.toEntity(dto);

        threshold.setTenantRule(tenantRule);
        threshold.setGlobalConditionId(globalCondition.getId());

        TenantRuleThreshold savedThreshold = tenantRuleThresholdRepo.save(threshold);

        TenantRuleThresholdResponseDto response = tenantRuleThresholdMapper.toResponseDto(savedThreshold);

        auditLogService.log(
                null,
                "TENANT_RULE_THRESHOLD",
                "CREATE",
                "TenantRuleThreshold",
                savedThreshold.getId(),
                null,
                response
        );

        return response;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPDATE_THRESHOLD_OVERRIDE", entityType = "RULE_THRESHOLD")
    public TenantRuleThresholdResponseDto updateThresholdOverride(UUID thresholdId, UpdateTenantRuleThresholdRequestDto dto) {
        TenantRuleThreshold threshold = tenantRuleThresholdRepo.findById(thresholdId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule Threshold not found with ID: " + thresholdId));

        TenantRuleThresholdResponseDto oldState = tenantRuleThresholdMapper.toResponseDto(threshold);
        tenantRuleThresholdMapper.updateEntityFromDto(dto, threshold);
        TenantRuleThreshold savedThreshold = tenantRuleThresholdRepo.save(threshold);
        TenantRuleThresholdResponseDto response = tenantRuleThresholdMapper.toResponseDto(savedThreshold);

        auditLogService.log(
                null,
                "TENANT_RULE_THRESHOLD",
                "UPDATE",
                "TenantRuleThreshold",
                threshold.getId(),
                oldState,
                response
        );

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_RULE_THRESHOLDS", entityType = "RULE_THRESHOLD")
    public List<TenantRuleThresholdResponseDto> getThresholdsForRule(UUID ruleId) {
        if (!tenantRuleRepo.existsById(ruleId)) {
            throw new EntityNotFoundException("Tenant Rule not found with ID: " + ruleId);
        }
        List<TenantRuleThreshold> thresholds = tenantRuleThresholdRepo.findByTenantRuleId(ruleId);
        return tenantRuleThresholdMapper.toResponseDtoList(thresholds);
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "DELETE_THRESHOLD_OVERRIDE", entityType = "RULE_THRESHOLD")
    public void deleteThresholdOverride(UUID thresholdId) {
        TenantRuleThreshold threshold = tenantRuleThresholdRepo.findById(thresholdId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule Threshold not found with ID: " + thresholdId));

        TenantRuleThresholdResponseDto oldState = tenantRuleThresholdMapper.toResponseDto(threshold);
        tenantRuleThresholdRepo.deleteById(thresholdId);

        auditLogService.log(
                null,
                "TENANT_RULE_THRESHOLD",
                "DELETE",
                "TenantRuleThreshold",
                thresholdId,
                oldState,
                null
        );
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "BULK_UPDATE_THRESHOLDS", entityType = "RULE_THRESHOLD")
    public List<TenantRuleThresholdResponseDto> updateThresholds(UUID ruleId, List<CreateTenantRuleThresholdRequestDto> overrides) {
        TenantRule rule = tenantRuleRepo.findById(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant Rule not found with ID: " + ruleId));

        tenantRuleThresholdRepo.deleteByTenantRuleId(ruleId);

        List<TenantRuleThreshold> newThresholds = overrides.stream()
                .map(dto -> {
                    TenantRuleThreshold threshold = tenantRuleThresholdMapper.toEntity(dto);
                    threshold.setTenantRule(rule);
                    return threshold;
                })
                .collect(Collectors.toList());

        List<TenantRuleThreshold> savedThresholds = tenantRuleThresholdRepo.saveAll(newThresholds);
        List<TenantRuleThresholdResponseDto> response = tenantRuleThresholdMapper.toResponseDtoList(savedThresholds);

        auditLogService.log(
                null,
                "TENANT_RULE_THRESHOLD",
                "BULK_UPDATE",
                "TenantRule",
                ruleId,
                null,
                response
        );

        return response;
    }
}
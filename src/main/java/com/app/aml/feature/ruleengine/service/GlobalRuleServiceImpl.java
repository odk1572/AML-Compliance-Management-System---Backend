package com.app.aml.feature.ruleengine.service;


import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.CreateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.UpdateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.CreateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.UpdateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import com.app.aml.feature.ruleengine.entity.GlobalRuleCondition;
import com.app.aml.feature.ruleengine.mapper.GlobalRuleConditionMapper;
import com.app.aml.feature.ruleengine.mapper.GlobalRuleMapper;
import com.app.aml.feature.ruleengine.repository.GlobalRuleConditionRepository;
import com.app.aml.feature.ruleengine.repository.GlobalRuleRepository;
import com.app.aml.annotation.AuditAction;
import com.app.aml.shared.audit.service.AuditLogService;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GlobalRuleServiceImpl implements GlobalRuleService {

    private final GlobalRuleRepository ruleRepo;
    private final GlobalRuleConditionRepository condRepo;
    private final GlobalRuleMapper ruleMapper;
    private final GlobalRuleConditionMapper condMapper;
    private final AuditLogService auditLog;

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "CREATE_GLOBAL_RULE", entityType = "GLOBAL_RULE")
    public GlobalRuleResponseDto createRule(CreateGlobalRuleRequestDto dto) {
        if (ruleRepo.existsByRuleNameAndSysIsDeletedFalse(dto.getRuleName())) {
            throw new EntityExistsException("Rule name already exists: " + dto.getRuleName());
        }

        GlobalRule rule = ruleMapper.toEntity(dto);
        GlobalRule savedRule = ruleRepo.save(rule);
        GlobalRuleResponseDto responseDto = ruleMapper.toResponseDto(savedRule);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "CREATE_GLOBAL_RULE",
                "GLOBAL_RULE",
                savedRule.getId(),
                null,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPDATE_GLOBAL_RULE", entityType = "GLOBAL_RULE")
    public GlobalRuleResponseDto updateRule(UUID id, UpdateGlobalRuleRequestDto dto) {
        GlobalRule rule = ruleRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Global Rule not found with ID: " + id));

        if (!rule.getRuleName().equalsIgnoreCase(dto.getRuleName()) &&
                ruleRepo.existsByRuleNameAndSysIsDeletedFalse(dto.getRuleName())) {
            throw new EntityExistsException("Rule name already exists: " + dto.getRuleName());
        }

        GlobalRuleResponseDto prevState = ruleMapper.toResponseDto(rule);
        ruleMapper.updateEntityFromDto(dto, rule);

        GlobalRule updatedRule = ruleRepo.save(rule);
        GlobalRuleResponseDto responseDto = ruleMapper.toResponseDto(updatedRule);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "UPDATE_GLOBAL_RULE",
                "GLOBAL_RULE",
                id,
                prevState,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "DELETE_GLOBAL_RULE", entityType = "GLOBAL_RULE")
    public void deleteRule(UUID id) {
        GlobalRule rule = ruleRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Global Rule not found with ID: " + id));

        GlobalRuleResponseDto prevState = ruleMapper.toResponseDto(rule);

        rule.setSysIsDeleted(true);
        rule.setSysDeletedAt(Instant.now());
        ruleRepo.save(rule);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "DELETE_GLOBAL_RULE",
                "GLOBAL_RULE",
                id,
                prevState,
                Map.of("status", "DELETED")
        );
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_GLOBAL_RULE", entityType = "GLOBAL_RULE")
    public GlobalRuleResponseDto getRuleById(UUID id) {
        return ruleRepo.findByIdAndSysIsDeletedFalse(id)
                .map(ruleMapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Global Rule not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "LIST_GLOBAL_RULES", entityType = "GLOBAL_RULE")
    public Page<GlobalRuleResponseDto> listRules(Pageable pageable) {
        return ruleRepo.findAllBySysIsDeletedFalse(pageable)
                .map(ruleMapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_RULE_ANALYTICS", entityType = "GLOBAL_RULE")
    public Page<Map<String, Object>> listRulesWithAlertCounts(Pageable pageable) {
        return ruleRepo.findAllRulesWithAlertCounts(pageable);
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "ADD_RULE_CONDITION", entityType = "RULE_CONDITION")
    public GlobalRuleConditionResponseDto addConditionToRule(CreateGlobalRuleConditionRequestDto dto) {
        if (!ruleRepo.existsByIdAndSysIsDeletedFalse(dto.getRuleId())) {
            throw new EntityNotFoundException("Global Rule not found with ID: " + dto.getRuleId());
        }

        GlobalRuleCondition condition = condMapper.toEntity(dto);
        GlobalRuleCondition savedCondition = condRepo.save(condition);
        GlobalRuleConditionResponseDto responseDto = condMapper.toResponseDto(savedCondition);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "ADD_RULE_CONDITION",
                "GLOBAL_RULE_CONDITION",
                savedCondition.getId(),
                null,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPDATE_RULE_CONDITION", entityType = "RULE_CONDITION")
    public GlobalRuleConditionResponseDto updateCondition(UUID conditionId, UpdateGlobalRuleConditionRequestDto dto) {
        GlobalRuleCondition condition = condRepo.findById(conditionId)
                .orElseThrow(() -> new EntityNotFoundException("Rule Condition not found with ID: " + conditionId));

        GlobalRuleConditionResponseDto prevState = condMapper.toResponseDto(condition);

        condMapper.updateEntityFromDto(dto, condition);

        GlobalRuleCondition updatedCondition = condRepo.save(condition);
        GlobalRuleConditionResponseDto responseDto = condMapper.toResponseDto(updatedCondition);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "UPDATE_RULE_CONDITION",
                "GLOBAL_RULE_CONDITION",
                conditionId,
                prevState,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "REMOVE_RULE_CONDITION", entityType = "RULE_CONDITION")
    public void removeCondition(UUID conditionId) {
        GlobalRuleCondition condition = condRepo.findById(conditionId)
                .orElseThrow(() -> new EntityNotFoundException("Rule Condition not found with ID: " + conditionId));

        GlobalRuleConditionResponseDto prevState = condMapper.toResponseDto(condition);

        condRepo.delete(condition);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "REMOVE_RULE_CONDITION",
                "GLOBAL_RULE_CONDITION",
                conditionId,
                prevState,
                Map.of("status", "DELETED")
        );
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_RULE_CONDITIONS", entityType = "RULE_CONDITION")
    public List<GlobalRuleConditionResponseDto> getConditionsByRuleId(UUID ruleId) {
        if (!ruleRepo.existsByIdAndSysIsDeletedFalse(ruleId)) {
            throw new EntityNotFoundException("Global Rule not found with ID: " + ruleId);
        }

        return condRepo.findByRuleId(ruleId).stream()
                .map(condMapper::toResponseDto)
                .toList();
    }
}
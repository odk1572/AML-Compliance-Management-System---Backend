package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.globalScenario.request.CreateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.request.UpdateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.response.GlobalScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.request.UpdateGlobalScenarioRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.response.GlobalScenarioRuleResponseDto;
import com.app.aml.feature.ruleengine.entity.GlobalRule;
import com.app.aml.feature.ruleengine.entity.GlobalScenario;
import com.app.aml.feature.ruleengine.entity.GlobalScenarioRule;
import com.app.aml.feature.ruleengine.mapper.GlobalScenarioMapper;
import com.app.aml.feature.ruleengine.mapper.GlobalScenarioRuleMapper;
import com.app.aml.feature.ruleengine.repository.GlobalRuleRepository;
import com.app.aml.feature.ruleengine.repository.GlobalScenarioRepository;
import com.app.aml.feature.ruleengine.repository.GlobalScenarioRuleRepository;
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
public class GlobalScenarioServiceImpl implements GlobalScenarioService {

    private final GlobalScenarioRepository scenarioRepo;
    private final GlobalScenarioRuleRepository scenarioRuleRepo;
    private final GlobalRuleRepository globalRuleRepo;
    private final GlobalScenarioMapper scenarioMapper;
    private final GlobalScenarioRuleMapper scenarioRuleMapper;
    private final AuditLogService auditLog;

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "CREATE_GLOBAL_SCENARIO", entityType = "GLOBAL_SCENARIO")
    public GlobalScenarioResponseDto createScenario(CreateGlobalScenarioRequestDto dto) {
        if (scenarioRepo.existsByScenarioNameAndSysIsDeletedFalse(dto.getScenarioName())) {
            throw new EntityExistsException("Global Scenario name already exists: " + dto.getScenarioName());
        }

        GlobalScenario scenario = scenarioMapper.toEntity(dto);
        GlobalScenario savedScenario = scenarioRepo.save(scenario);
        GlobalScenarioResponseDto responseDto = scenarioMapper.toResponseDto(savedScenario);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "CREATE_GLOBAL_SCENARIO",
                "GLOBAL_SCENARIO",
                savedScenario.getId(),
                null,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPDATE_GLOBAL_SCENARIO", entityType = "GLOBAL_SCENARIO")
    public GlobalScenarioResponseDto updateScenario(UUID id, UpdateGlobalScenarioRequestDto dto) {
        GlobalScenario scenario = scenarioRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Global Scenario not found with ID: " + id));

        if (!scenario.getScenarioName().equalsIgnoreCase(dto.getScenarioName()) &&
                scenarioRepo.existsByScenarioNameAndSysIsDeletedFalse(dto.getScenarioName())) {
            throw new EntityExistsException("Global Scenario name already exists: " + dto.getScenarioName());
        }

        GlobalScenarioResponseDto prevState = scenarioMapper.toResponseDto(scenario);
        scenarioMapper.updateEntityFromDto(dto, scenario);

        GlobalScenario updatedScenario = scenarioRepo.save(scenario);
        GlobalScenarioResponseDto responseDto = scenarioMapper.toResponseDto(updatedScenario);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "UPDATE_GLOBAL_SCENARIO",
                "GLOBAL_SCENARIO",
                id,
                prevState,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "DELETE_GLOBAL_SCENARIO", entityType = "GLOBAL_SCENARIO")
    public void deleteScenario(UUID id) {
        GlobalScenario scenario = scenarioRepo.findByIdAndSysIsDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Global Scenario not found with ID: " + id));

        GlobalScenarioResponseDto prevState = scenarioMapper.toResponseDto(scenario);

        scenario.setSysIsDeleted(true);
        scenario.setSysDeletedAt(Instant.now());
        scenarioRepo.save(scenario);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "DELETE_GLOBAL_SCENARIO",
                "GLOBAL_SCENARIO",
                id,
                prevState,
                Map.of("status", "DELETED")
        );
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_GLOBAL_SCENARIO", entityType = "GLOBAL_SCENARIO")
    public GlobalScenarioResponseDto getScenarioById(UUID id) {
        return scenarioRepo.findByIdAndSysIsDeletedFalse(id)
                .map(scenarioMapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Global Scenario not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "LIST_GLOBAL_SCENARIOS", entityType = "GLOBAL_SCENARIO")
    public Page<GlobalScenarioResponseDto> listScenarios(Pageable pageable) {
        return scenarioRepo.findAllBySysIsDeletedFalse(pageable)
                .map(scenarioMapper::toResponseDto);
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "ADD_RULE_TO_SCENARIO", entityType = "GLOBAL_SCENARIO_RULE")
    public GlobalScenarioRuleResponseDto addRuleToScenario(UUID scenarioId, UUID ruleId, Integer priority) {
        GlobalScenario scenario = scenarioRepo.findByIdAndSysIsDeletedFalse(scenarioId)
                .orElseThrow(() -> new EntityNotFoundException("Global Scenario not found with ID: " + scenarioId));

        GlobalRule rule = globalRuleRepo.findByIdAndSysIsDeletedFalse(ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Global Rule not found with ID: " + ruleId));

        if (scenarioRuleRepo.existsByScenarioIdAndRuleId(scenarioId, ruleId)) {
            throw new EntityExistsException("Rule is already mapped to this scenario.");
        }

        GlobalScenarioRule scenarioRule = new GlobalScenarioRule();
        scenarioRule.setScenario(scenario);
        scenarioRule.setRule(rule);
        scenarioRule.setPriorityOrder(priority != null ? priority : 0);
        scenarioRule.setActive(true);

        GlobalScenarioRule savedScenarioRule = scenarioRuleRepo.save(scenarioRule);
        GlobalScenarioRuleResponseDto responseDto = scenarioRuleMapper.toResponseDto(savedScenarioRule);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "ADD_RULE_TO_SCENARIO",
                "GLOBAL_SCENARIO_RULE",
                savedScenarioRule.getId(),
                null,
                responseDto
        );

        return responseDto;
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "REMOVE_RULE_FROM_SCENARIO", entityType = "GLOBAL_SCENARIO_RULE")
    public void removeRuleFromScenario(UUID scenarioId, UUID ruleId) {
        GlobalScenarioRule scenarioRule = scenarioRuleRepo.findByScenarioIdAndRuleId(scenarioId, ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Mapping not found for Scenario ID: " + scenarioId + " and Rule ID: " + ruleId));

        GlobalScenarioRuleResponseDto prevState = scenarioRuleMapper.toResponseDto(scenarioRule);

        scenarioRuleRepo.delete(scenarioRule);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "REMOVE_RULE_FROM_SCENARIO",
                "GLOBAL_SCENARIO_RULE",
                scenarioRule.getId(),
                prevState,
                Map.of("status", "DELETED")
        );
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_SCENARIO_RULES", entityType = "GLOBAL_SCENARIO_RULE")
    public List<GlobalScenarioRuleResponseDto> getRulesByScenarioId(UUID scenarioId) {
        if (!scenarioRepo.existsByIdAndSysIsDeletedFalse(scenarioId)) {
            throw new EntityNotFoundException("Global Scenario not found with ID: " + scenarioId);
        }

        return scenarioRuleRepo.findByScenarioIdOrderByPriorityOrderAsc(scenarioId).stream()
                .map(scenarioRuleMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPDATE_SCENARIO_RULE_MAPPING", entityType = "GLOBAL_SCENARIO_RULE")
    public GlobalScenarioRuleResponseDto updateRuleInScenario(UUID scenarioId, UUID ruleId, UpdateGlobalScenarioRuleRequestDto dto) {
        GlobalScenarioRule scenarioRule = scenarioRuleRepo.findByScenarioIdAndRuleId(scenarioId, ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Mapping not found for Scenario ID: " + scenarioId + " and Rule ID: " + ruleId));

        GlobalScenarioRuleResponseDto prevState = scenarioRuleMapper.toResponseDto(scenarioRule);

        if (dto.getIsActive() != null) {
            scenarioRule.setActive(dto.getIsActive());
        }

        if (dto.getPriorityOrder() != null) {
            scenarioRule.setPriorityOrder(dto.getPriorityOrder());
        }

        GlobalScenarioRule updatedScenarioRule = scenarioRuleRepo.save(scenarioRule);

        GlobalScenarioRuleResponseDto responseDto = scenarioRuleMapper.toResponseDto(updatedScenarioRule);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "UPDATE_SCENARIO_RULE",
                "GLOBAL_SCENARIO_RULE",
                updatedScenarioRule.getId(),
                prevState,
                responseDto
        );

        return responseDto;
    }
}
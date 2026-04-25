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
    public GlobalScenarioResponseDto getScenarioById(UUID id) {
        return scenarioRepo.findByIdAndSysIsDeletedFalse(id)
                .map(scenarioMapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Global Scenario not found with ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GlobalScenarioResponseDto> listScenarios(Pageable pageable) {
        return scenarioRepo.findAllBySysIsDeletedFalse(pageable)
                .map(scenarioMapper::toResponseDto);
    }

    @Override
    @Transactional
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
    public GlobalScenarioRuleResponseDto updateRuleInScenario(UUID scenarioId, UUID ruleId, UpdateGlobalScenarioRuleRequestDto dto) {
        // 1. Find the existing entity
        GlobalScenarioRule scenarioRule = scenarioRuleRepo.findByScenarioIdAndRuleId(scenarioId, ruleId)
                .orElseThrow(() -> new EntityNotFoundException("Mapping not found for Scenario ID: " + scenarioId + " and Rule ID: " + ruleId));

        // 2. Capture previous state for Audit Log
        GlobalScenarioRuleResponseDto prevState = scenarioRuleMapper.toResponseDto(scenarioRule);

        // 3. Debug Log: Verify if Postman data reached the DTO

        // 4. Manual Update (Replaces the Mapper call)
        // We check for null to mimic the 'IGNORE' strategy
        if (dto.getIsActive() != null) {
            scenarioRule.setActive(dto.getIsActive());
        }

        if (dto.getPriorityOrder() != null) {
            scenarioRule.setPriorityOrder(dto.getPriorityOrder());
        }

        // 5. Persist the changes
        GlobalScenarioRule updatedScenarioRule = scenarioRuleRepo.save(scenarioRule);

        // 6. Map to Response DTO
        GlobalScenarioRuleResponseDto responseDto = scenarioRuleMapper.toResponseDto(updatedScenarioRule);

        // 7. Log to Platform Audit
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
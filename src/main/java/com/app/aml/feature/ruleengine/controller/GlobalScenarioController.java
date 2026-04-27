package com.app.aml.feature.ruleengine.controller;


import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ruleengine.dto.globalScenario.request.CreateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.request.UpdateGlobalScenarioRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenario.response.GlobalScenarioResponseDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.request.UpdateGlobalScenarioRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalScenarioRules.response.GlobalScenarioRuleResponseDto;
import com.app.aml.feature.ruleengine.service.GlobalScenarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/scenarios")
@RequiredArgsConstructor
public class GlobalScenarioController {

    private final GlobalScenarioService globalScenarioService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalScenarioResponseDto>> createScenario(
            @Valid @RequestBody CreateGlobalScenarioRequestDto requestDto,
            HttpServletRequest request) {

        GlobalScenarioResponseDto data = globalScenarioService.createScenario(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Global Scenario created successfully",
                        request.getRequestURI(),
                        data
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalScenarioResponseDto>> updateScenario(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGlobalScenarioRequestDto requestDto,
            HttpServletRequest request) {

        GlobalScenarioResponseDto data = globalScenarioService.updateScenario(id, requestDto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Scenario updated successfully",
                request.getRequestURI(),
                data
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteScenario(
            @PathVariable UUID id,
            HttpServletRequest request) {

        globalScenarioService.deleteScenario(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Scenario deleted successfully",
                request.getRequestURI(),
                null
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalScenarioResponseDto>> getScenarioById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GlobalScenarioResponseDto data = globalScenarioService.getScenarioById(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Scenario retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<Page<GlobalScenarioResponseDto>>> listScenarios(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<GlobalScenarioResponseDto> data = globalScenarioService.listScenarios(pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Scenarios retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @PostMapping("/{scenarioId}/rules/{ruleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalScenarioRuleResponseDto>> addRuleToScenario(
            @PathVariable UUID scenarioId,
            @PathVariable UUID ruleId,
            @RequestParam(required = false) Integer priority,
            HttpServletRequest request) {

        GlobalScenarioRuleResponseDto data = globalScenarioService.addRuleToScenario(scenarioId, ruleId, priority);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Rule mapped to Scenario successfully",
                        request.getRequestURI(),
                        data
                ));
    }

    @DeleteMapping("/{scenarioId}/rules/{ruleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeRuleFromScenario(
            @PathVariable UUID scenarioId,
            @PathVariable UUID ruleId,
            HttpServletRequest request) {

        globalScenarioService.removeRuleFromScenario(scenarioId, ruleId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Rule removed from Scenario successfully",
                request.getRequestURI(),
                null
        ));
    }

    @GetMapping("/{scenarioId}/rules")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<List<GlobalScenarioRuleResponseDto>>> getRulesByScenarioId(
            @PathVariable UUID scenarioId,
            HttpServletRequest request) {

        List<GlobalScenarioRuleResponseDto> data = globalScenarioService.getRulesByScenarioId(scenarioId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario Rules retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @PutMapping("/{scenarioId}/rules/{ruleId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalScenarioRuleResponseDto>> updateRuleInScenario(
            @PathVariable UUID scenarioId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody UpdateGlobalScenarioRuleRequestDto requestDto,
            HttpServletRequest request) {

        GlobalScenarioRuleResponseDto data = globalScenarioService.updateRuleInScenario(scenarioId, ruleId, requestDto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Scenario Rule mapping updated successfully",
                request.getRequestURI(),
                data
        ));
    }
}
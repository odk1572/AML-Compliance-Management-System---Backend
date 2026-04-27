package com.app.aml.feature.ruleengine.controller;
import com.app.aml.apiResponse.ApiResponse;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.CreateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.request.UpdateGlobalRuleConditionRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRuleCondition.response.GlobalRuleConditionResponseDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.CreateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.request.UpdateGlobalRuleRequestDto;
import com.app.aml.feature.ruleengine.dto.globalRules.response.GlobalRuleResponseDto;
import com.app.aml.feature.ruleengine.service.GlobalRuleService;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/platform/rules")
@RequiredArgsConstructor
public class GlobalRuleController {

    private final GlobalRuleService globalRuleService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalRuleResponseDto>> createRule(
            @Valid @RequestBody CreateGlobalRuleRequestDto requestDto,
            HttpServletRequest request) {

        GlobalRuleResponseDto data = globalRuleService.createRule(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Global Rule created successfully",
                        request.getRequestURI(),
                        data
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalRuleResponseDto>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateGlobalRuleRequestDto requestDto,
            HttpServletRequest request) {

        GlobalRuleResponseDto data = globalRuleService.updateRule(id, requestDto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rule updated successfully",
                request.getRequestURI(),
                data
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @PathVariable UUID id,
            HttpServletRequest request) {

        globalRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rule deleted successfully",
                request.getRequestURI(),
                null
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalRuleResponseDto>> getRuleById(
            @PathVariable UUID id,
            HttpServletRequest request) {

        GlobalRuleResponseDto data = globalRuleService.getRuleById(id);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rule retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<Page<GlobalRuleResponseDto>>> listRules(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<GlobalRuleResponseDto> data = globalRuleService.listRules(pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rules retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping("/with-alert-counts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> listRulesWithAlertCounts(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<Map<String, Object>> data = globalRuleService.listRulesWithAlertCounts(pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rules with alert counts retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @PostMapping("/conditions")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalRuleConditionResponseDto>> addConditionToRule(
            @Valid @RequestBody CreateGlobalRuleConditionRequestDto requestDto,
            HttpServletRequest request) {

        GlobalRuleConditionResponseDto data = globalRuleService.addConditionToRule(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(
                        HttpStatus.CREATED,
                        "Condition added to Global Rule successfully",
                        request.getRequestURI(),
                        data
                ));
    }

    @PutMapping("/conditions/{conditionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GlobalRuleConditionResponseDto>> updateCondition(
            @PathVariable UUID conditionId,
            @Valid @RequestBody UpdateGlobalRuleConditionRequestDto requestDto,
            HttpServletRequest request) {

        GlobalRuleConditionResponseDto data = globalRuleService.updateCondition(conditionId, requestDto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rule Condition updated successfully",
                request.getRequestURI(),
                data
        ));
    }

    @DeleteMapping("/conditions/{conditionId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> removeCondition(
            @PathVariable UUID conditionId,
            HttpServletRequest request) {

        globalRuleService.removeCondition(conditionId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rule Condition removed successfully",
                request.getRequestURI(),
                null
        ));
    }

    @GetMapping("/{ruleId}/conditions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<ApiResponse<List<GlobalRuleConditionResponseDto>>> getConditionsByRuleId(
            @PathVariable UUID ruleId,
            HttpServletRequest request) {

        List<GlobalRuleConditionResponseDto> data = globalRuleService.getConditionsByRuleId(ruleId);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Global Rule Conditions retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }
}
package com.app.aml.feature.ruleengine.controller;

import com.app.aml.domain.api.ApiResponse;
import com.app.aml.feature.ruleengine.dto.geographicRiskRating.request.CreateGeographicRiskRequestDto;
import com.app.aml.feature.ruleengine.dto.geographicRiskRating.response.GeographicRiskRatingResponseDto;
import com.app.aml.feature.ruleengine.service.GeographicRiskService;
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

@RestController
@RequestMapping("/api/v1/platform/geo-risk")
@RequiredArgsConstructor
public class GeographicRiskController {

    private final GeographicRiskService geoRiskService;

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<GeographicRiskRatingResponseDto>> upsertRating(
            @Valid @RequestBody CreateGeographicRiskRequestDto requestDto,
            HttpServletRequest request) {

        GeographicRiskRatingResponseDto data = geoRiskService.upsertRating(requestDto);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Geographic risk rating processed successfully",
                request.getRequestURI(),
                data
        ));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<List<GeographicRiskRatingResponseDto>>> bulkUpsert(
            @Valid @RequestBody List<CreateGeographicRiskRequestDto> requestDtos,
            HttpServletRequest request) {

        List<GeographicRiskRatingResponseDto> data = geoRiskService.bulkUpsert(requestDtos);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Bulk geographic risk ratings processed successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<Page<GeographicRiskRatingResponseDto>>> listRatings(
            @PageableDefault(size = 20) Pageable pageable,
            HttpServletRequest request) {

        Page<GeographicRiskRatingResponseDto> data = geoRiskService.listRatings(pageable);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Geographic risk ratings retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @GetMapping("/{countryCode}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<ApiResponse<GeographicRiskRatingResponseDto>> getRating(
            @PathVariable String countryCode,
            HttpServletRequest request) {

        GeographicRiskRatingResponseDto data = geoRiskService.getRating(countryCode);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Geographic risk rating retrieved successfully",
                request.getRequestURI(),
                data
        ));
    }

    @DeleteMapping("/{countryCode}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteRating(
            @PathVariable String countryCode,
            HttpServletRequest request) {

        geoRiskService.deleteRating(countryCode);
        return ResponseEntity.ok(ApiResponse.of(
                HttpStatus.OK,
                "Geographic risk rating deleted successfully",
                request.getRequestURI(),
                null
        ));
    }
}
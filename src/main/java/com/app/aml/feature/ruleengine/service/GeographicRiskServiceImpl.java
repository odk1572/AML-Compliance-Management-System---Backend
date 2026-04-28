package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.geographicRiskRating.request.CreateGeographicRiskRequestDto;
import com.app.aml.feature.ruleengine.dto.geographicRiskRating.response.GeographicRiskRatingResponseDto;
import com.app.aml.feature.ruleengine.entity.GeographicRiskRating;
import com.app.aml.feature.ruleengine.mapper.GeographicRiskRatingMapper;
import com.app.aml.feature.ruleengine.repository.GeographicRiskRatingRepository;
import com.app.aml.annotation.AuditAction;
import com.app.aml.audit.service.AuditLogService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeographicRiskServiceImpl implements GeographicRiskService {

    private final GeographicRiskRatingRepository repo;
    private final GeographicRiskRatingMapper mapper;
    private final AuditLogService auditLog;

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "UPSERT_GEO_RISK", entityType = "GEO_RISK")
    public GeographicRiskRatingResponseDto upsertRating(CreateGeographicRiskRequestDto dto) {
        return repo.findByCountryCodeAndSysIsDeletedFalse(dto.getCountryCode())
                .map(existingEntity -> updateExistingRating(existingEntity, dto))
                .orElseGet(() -> createNewRating(dto));
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "BULK_UPSERT_GEO_RISK", entityType = "GEO_RISK")
    public List<GeographicRiskRatingResponseDto> bulkUpsert(List<CreateGeographicRiskRequestDto> dtos) {
        return dtos.stream()
                .map(this::upsertRating)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "LIST_GEO_RISK_RATINGS", entityType = "GEO_RISK")
    public Page<GeographicRiskRatingResponseDto> listRatings(Pageable pageable) {
        return repo.findAllBySysIsDeletedFalse(pageable)
                .map(mapper::toResponseDto);
    }

    @Override
    @Transactional(readOnly = true)
    @AuditAction(category = "DATA_ACCESS", action = "VIEW_GEO_RISK_DETAIL", entityType = "GEO_RISK")
    public GeographicRiskRatingResponseDto getRating(String countryCode) {
        return repo.findByCountryCodeAndSysIsDeletedFalse(countryCode.toUpperCase())
                .map(mapper::toResponseDto)
                .orElseThrow(() -> new EntityNotFoundException("Geographic risk rating not found for: " + countryCode));
    }

    @Override
    @Transactional
    @AuditAction(category = "RULE_ENGINE", action = "DELETE_GEO_RISK", entityType = "GEO_RISK")
    public void deleteRating(String countryCode) {
        GeographicRiskRating entity = repo.findByCountryCodeAndSysIsDeletedFalse(countryCode.toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Geographic risk rating not found for: " + countryCode));

        GeographicRiskRatingResponseDto prevState = mapper.toResponseDto(entity);

        entity.setSysIsDeleted(true);
        entity.setSysDeletedAt(Instant.now());
        repo.save(entity);

        auditLog.logPlatform(
                null,
                "RULE_ENGINE",
                "DELETE_GEO_RISK",
                "GEO_RISK",
                entity.getId(),
                prevState,
                java.util.Map.of("status", "DELETED")
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(String countryCode) {
        return repo.existsByCountryCodeAndSysIsDeletedFalse(countryCode.toUpperCase());
    }

    private GeographicRiskRatingResponseDto createNewRating(CreateGeographicRiskRequestDto dto) {
        GeographicRiskRating entity = mapper.toEntity(dto);
        entity.setCountryCode(dto.getCountryCode().toUpperCase());
        GeographicRiskRating saved = repo.save(entity);

        GeographicRiskRatingResponseDto response = mapper.toResponseDto(saved);
        auditLog.logPlatform(null, "RULE_ENGINE", "CREATE_GEO_RISK", "GEO_RISK", saved.getId(), null, response);

        return response;
    }

    private GeographicRiskRatingResponseDto updateExistingRating(GeographicRiskRating entity, CreateGeographicRiskRequestDto dto) {
        GeographicRiskRatingResponseDto prevState = mapper.toResponseDto(entity);

        entity.setCountryName(dto.getCountryName());
        entity.setFatfStatus(dto.getFatfStatus());
        entity.setBaselAmlIndexScore(dto.getBaselAmlIndexScore());
        entity.setRiskTier(dto.getRiskTier());
        entity.setNotes(dto.getNotes());
        entity.setEffectiveFrom(dto.getEffectiveFrom());

        GeographicRiskRating updated = repo.save(entity);
        GeographicRiskRatingResponseDto nextState = mapper.toResponseDto(updated);

        auditLog.logPlatform(null, "RULE_ENGINE", "UPDATE_GEO_RISK", "GEO_RISK", updated.getId(), prevState, nextState);

        return nextState;
    }
}
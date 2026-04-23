package com.app.aml.feature.ruleengine.service;

import com.app.aml.feature.ruleengine.dto.geographicRiskRating.request.CreateGeographicRiskRequestDto;
import com.app.aml.feature.ruleengine.dto.geographicRiskRating.response.GeographicRiskRatingResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GeographicRiskService {

    GeographicRiskRatingResponseDto upsertRating(CreateGeographicRiskRequestDto dto);

    List<GeographicRiskRatingResponseDto> bulkUpsert(List<CreateGeographicRiskRequestDto> dtos);

    Page<GeographicRiskRatingResponseDto> listRatings(Pageable pageable);

    GeographicRiskRatingResponseDto getRating(String countryCode);

    void deleteRating(String countryCode);

    boolean exists(String countryCode);
}
package com.app.aml.feature.alert.dto.alert.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlertCustomerDto {
    private UUID id;
    private String accountNumber;
    private String customerName;
    private String riskRating;
    // Add any other fields you want visible on the UI dashboard
}
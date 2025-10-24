package com.mobile.aura.dto.weight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a single weight data point.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeightPointResp {

    /**
     * Date in format "yyyy-MM-dd"
     */
    private String date;

    /**
     * Weight in kilograms
     */
    private Double weightKg;
}
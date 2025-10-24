package com.mobile.aura.dto.water;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for water configuration.
 * Contains quick records, goal intake, and current intake.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WaterConfigResp {

    /**
     * Quick record amounts in milliliters (max 4 items)
     */
    private List<Integer> quickRecordsMl;

    /**
     * Daily water intake goal in milliliters
     */
    private Integer goalWaterIntakeMl;

    /**
     * Current day's water intake in milliliters
     */
    private Integer currentIntakeMl;
}

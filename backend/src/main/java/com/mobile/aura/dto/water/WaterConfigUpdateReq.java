package com.mobile.aura.dto.water;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Request DTO for updating water configuration.
 */
@Data
public class WaterConfigUpdateReq {

    /**
     * Quick record amounts in milliliters (max 4 items)
     */
    @Size(max = 4, message = "Quick records cannot exceed 4 items")
    private List<@Min(1) @Max(10000) Integer> quickRecordsMl;

    /**
     * Daily water intake goal in milliliters
     */
    @Min(value = 500, message = "Goal must be at least 500ml")
    @Max(value = 10000, message = "Goal cannot exceed 10000ml")
    private Integer goalWaterIntakeMl;
}

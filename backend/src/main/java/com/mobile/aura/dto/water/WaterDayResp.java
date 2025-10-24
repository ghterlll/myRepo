package com.mobile.aura.dto.water;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for daily water intake.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WaterDayResp {

    /**
     * Date in format "yyyy-MM-dd"
     */
    private String date;

    /**
     * Total water intake for the day in milliliters
     */
    private Integer amountMl;
}

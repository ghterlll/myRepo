package com.mobile.aura.dto.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for step count range query.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepRangeResp {

    /**
     * List of daily step counts
     */
    private List<StepDayResp> items;

    /**
     * Total steps for the period
     */
    private Integer totalSteps;

    /**
     * Total distance in kilometers (formatted string)
     */
    private String totalDistanceKm;

    /**
     * Total calories burned
     */
    private Integer totalKcal;

    /**
     * Average daily steps
     */
    private Integer avgDailySteps;
}

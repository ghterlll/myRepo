package com.mobile.aura.dto.exercise;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for exercise range query.
 * Contains daily workout summaries and aggregate statistics for the period.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExerciseRangeResp {

    /**
     * List of daily workout summaries
     */
    private List<DailyWorkoutResp> items;

    /**
     * Total calories burned for the period
     */
    private Integer totalKcal;

    /**
     * Total exercise minutes for the period
     */
    private Integer totalMinutes;

    /**
     * Total distance covered in kilometers (if applicable)
     */
    private BigDecimal totalDistanceKm;

    /**
     * Average daily calories burned
     */
    private Integer avgDailyKcal;

    /**
     * Number of active days (days with at least one exercise)
     */
    private Integer activeDays;
}

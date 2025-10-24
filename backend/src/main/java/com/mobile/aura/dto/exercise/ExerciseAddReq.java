package com.mobile.aura.dto.exercise;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * Request DTO for adding exercise log.
 * Simplified - directly records exercise information without item tables.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExerciseAddReq extends SingleDateReq {

    /**
     * Exercise name (e.g., "Running", "Jogging")
     * Optional - defaults to "Running"
     */
    private String exerciseName;

    /**
     * Duration in minutes
     */
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer minutes;

    /**
     * Distance in kilometers
     * Optional - for running exercises
     */
    @DecimalMin(value = "0.01", message = "Distance must be at least 0.01 km")
    private BigDecimal distanceKm;

    /**
     * Calories burned
     * Optional - can be calculated or provided by user
     */
    @Min(value = 0, message = "Calories must be non-negative")
    private Integer kcal;
}

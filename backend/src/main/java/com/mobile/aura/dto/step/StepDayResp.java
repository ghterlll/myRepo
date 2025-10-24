package com.mobile.aura.dto.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for daily step count.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepDayResp {

    /**
     * Date in format "yyyy-MM-dd"
     */
    private String date;

    /**
     * Step count
     */
    private Integer steps;

    /**
     * Distance in kilometers (formatted string)
     */
    private String distanceKm;

    /**
     * Calories burned
     */
    private Integer kcal;

    /**
     * Active minutes
     */
    private Integer activeMinutes;

    /**
     * Data source
     */
    private String dataSource;

    /**
     * Server version number
     */
    private Integer version;

    /**
     * Sync sequence number
     */
    private Long syncSequence;
}

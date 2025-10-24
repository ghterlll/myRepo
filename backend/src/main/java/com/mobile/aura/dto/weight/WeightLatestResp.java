package com.mobile.aura.dto.weight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for latest, initial, and target weight information.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeightLatestResp {

    /**
     * Latest weight record date in format "yyyy-MM-dd"
     */
    private String latestDate;

    /**
     * Latest weight in kilograms
     */
    private Double latestWeightKg;

    /**
     * Initial weight record date in format "yyyy-MM-dd"
     */
    private String initialDate;

    /**
     * Initial weight in kilograms
     */
    private Double initialWeightKg;

    /**
     * Target weight in kilograms
     */
    private Double targetWeightKg;

    /**
     * Target deadline date in format "yyyy-MM-dd"
     */
    private String targetDate;
}
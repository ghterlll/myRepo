package com.mobile.aura.dto.weight;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for weight records within a date range.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WeightRangeResp {

    /**
     * List of weight data points
     */
    private List<WeightPointResp> items;
}
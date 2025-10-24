package com.mobile.aura.dto.water;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for water intake range query.
 * Contains daily water intake records for a date range.
 * Missing dates are filled with 0 ml.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WaterRangeResp {

    /**
     * List of daily water intake records
     */
    private List<WaterDayResp> items;
}

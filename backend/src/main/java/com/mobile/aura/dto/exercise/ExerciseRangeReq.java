package com.mobile.aura.dto.exercise;

import com.mobile.aura.dto.DateRangeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for querying exercise logs in a date range.
 * Maximum range is 31 days.
 * Defaults to 7 days ago to today if dates not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExerciseRangeReq extends DateRangeReq {
    // Date range fields inherited from DateRangeReq (from/to)
    // Service layer will validate that range is within 31 days
}

package com.mobile.aura.dto.step;

import com.mobile.aura.dto.DateRangeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for querying step counts in a date range.
 * Defaults to 7 days ago to today if dates not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StepRangeReq extends DateRangeReq {
    // Date range fields inherited from DateRangeReq (from/to)
}

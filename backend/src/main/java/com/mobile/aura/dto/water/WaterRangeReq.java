package com.mobile.aura.dto.water;

import com.mobile.aura.dto.DateRangeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for querying water intake range.
 * Defaults to 6 days ago to today if dates not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WaterRangeReq extends DateRangeReq {
    // Date range fields inherited from DateRangeReq (from/to)
}

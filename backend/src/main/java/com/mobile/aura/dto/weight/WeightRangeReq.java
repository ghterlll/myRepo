package com.mobile.aura.dto.weight;

import com.mobile.aura.dto.DateRangeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for querying weight records within a date range.
 * Defaults to today if dates not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeightRangeReq extends DateRangeReq {
    // Date range fields inherited from DateRangeReq (from/to)
    // Note: Previous API used 'start/end' but now unified to 'from/to'
}

package com.mobile.aura.dto.water;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for submitting/overwriting water intake.
 * Sets the total water intake for the specified date (replaces existing value).
 * Date defaults to today if not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WaterSubmitReq extends SingleDateReq {

    /**
     * Total water intake in milliliters
     * Must be between 0 and 100,000 ml
     */
    @NotNull(message = "Amount is required")
    @Min(value = 0, message = "Amount must be non-negative")
    @Max(value = 100_000, message = "Amount must not exceed 100,000 ml")
    private Integer amountMl;
}

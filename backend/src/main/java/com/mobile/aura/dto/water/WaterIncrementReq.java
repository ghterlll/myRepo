package com.mobile.aura.dto.water;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for incrementing water intake.
 * Adds the specified amount to the existing water intake for the specified date.
 * Date defaults to today if not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WaterIncrementReq extends SingleDateReq {

    /**
     * Amount of water to add in milliliters
     * Must be positive
     */
    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be at least 1ml")
    private Integer amountMl;
}

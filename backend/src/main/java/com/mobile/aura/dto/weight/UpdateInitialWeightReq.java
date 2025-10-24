package com.mobile.aura.dto.weight;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for updating initial weight.
 * Allows users to correct their initial weight if they made a mistake.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateInitialWeightReq extends SingleDateReq {

    /**
     * Initial weight in kilograms
     * Must be between 1 and 500 kg
     */
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "1.0", message = "Weight must be at least 1 kg")
    @DecimalMax(value = "500.0", message = "Weight must not exceed 500 kg")
    private Double weightKg;
}

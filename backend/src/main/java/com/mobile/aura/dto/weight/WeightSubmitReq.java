package com.mobile.aura.dto.weight;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for submitting weight record.
 * Date defaults to today if not provided.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class WeightSubmitReq extends SingleDateReq {

    /**
     * Weight in kilograms
     * Must be between 1 and 500 kg
     */
    @NotNull(message = "Weight is required")
    @DecimalMin(value = "1.0", message = "Weight must be at least 1 kg")
    @DecimalMax(value = "500.0", message = "Weight must not exceed 500 kg")
    private Double weightKg;

    /**
     * Optional note for the weight record
     * Maximum length: 500 characters
     */
    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;
}
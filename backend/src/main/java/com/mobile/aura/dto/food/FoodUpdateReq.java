package com.mobile.aura.dto.food;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating a custom food item.
 * All fields are OPTIONAL - only provided fields will be updated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodUpdateReq {

    /**
     * Food name
     * OPTIONAL
     */
    private String name;

    /**
     * Unit of measurement
     * OPTIONAL
     */
    private String unitName;

    /**
     * Calories per unit
     * OPTIONAL
     */
    @Min(value = 0, message = "Calories must be non-negative")
    private Integer kcalPerUnit;

    /**
     * Image URL
     * OPTIONAL - can update image by uploading new one first
     */
    private String imageUrl;
}
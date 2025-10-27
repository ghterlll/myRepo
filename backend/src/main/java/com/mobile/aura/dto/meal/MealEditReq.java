package com.mobile.aura.dto.meal;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for editing an existing meal log.
 * All fields are optional - only provided fields will be updated.
 * Date can be changed to move meal to a different day.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MealEditReq extends SingleDateReq {

    /**
     * Meal type: 0=breakfast, 1=lunch, 2=dinner, 3=snack
     * OPTIONAL - only update if provided
     */
    @Min(value = 0, message = "Meal type must be between 0 and 3")
    @Max(value = 3, message = "Meal type must be between 0 and 3")
    private Integer mealType;

    /**
     * Food item name
     * OPTIONAL - only update if provided
     */
    private String itemName;

    /**
     * Unit name (e.g., "unit", "serving", "gram", "piece")
     * OPTIONAL - only update if provided
     */
    private String unitName;

    /**
     * Quantity of units consumed
     * OPTIONAL - only update if provided
     */
    @Positive(message = "Unit quantity must be positive")
    private Double unitQty;

    /**
     * Total calories
     * OPTIONAL - only update if provided
     */
    @Min(value = 0, message = "Calories must be non-negative")
    private Integer kcal;

    /**
     * Image URL from OSS
     * OPTIONAL - only update if provided
     */
    private String imageUrl;
}

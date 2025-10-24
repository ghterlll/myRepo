package com.mobile.aura.dto.meal;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for adding a meal from an existing food source.
 * Only requires sourceId - all food details will be fetched from database.
 * Date defaults to today if not provided (allows backfilling historical meals).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MealAddFromSourceReq extends SingleDateReq {

    /**
     * Meal type: 0=breakfast, 1=lunch, 2=dinner, 3=snack
     */
    @NotNull(message = "Meal type is required")
    @Min(value = 0, message = "Meal type must be between 0 and 3")
    @Max(value = 3, message = "Meal type must be between 0 and 3")
    private Integer mealType;

    /**
     * Source ID (references user_food_item)
     */
    @NotNull(message = "Source ID is required")
    private Long sourceId;

    /**
     * Quantity of units consumed
     * OPTIONAL - defaults to 1.0 if not provided
     */
    @Positive(message = "Unit quantity must be positive")
    private Double unitQty;
}

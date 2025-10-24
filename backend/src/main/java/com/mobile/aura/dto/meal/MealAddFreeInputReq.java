package com.mobile.aura.dto.meal;

import com.mobile.aura.dto.SingleDateReq;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for adding a meal with free input (no food source).
 * Client provides all meal details including name, calories, and image.
 * Date defaults to today if not provided (allows backfilling historical meals).
 * <p>
 * Flow:
 * 1. Client uploads food image via POST /files/food/image -> gets imageUrl
 * 2. Client calls YOLO CV service to get kcal
 * 3. Client sends this request with all required fields
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MealAddFreeInputReq extends SingleDateReq {

    /**
     * Meal type: 0=breakfast, 1=lunch, 2=dinner, 3=snack
     */
    @NotNull(message = "Meal type is required")
    @Min(value = 0, message = "Meal type must be between 0 and 3")
    @Max(value = 3, message = "Meal type must be between 0 and 3")
    private Integer mealType;

    /**
     * Food item name
     */
    @NotBlank(message = "Item name is required")
    private String itemName;

    /**
     * Unit name (e.g., "unit", "serving", "gram", "piece")
     * OPTIONAL - defaults to "unit" if not provided
     */
    private String unitName;

    /**
     * Quantity of units consumed
     * OPTIONAL - defaults to 1.0 if not provided
     */
    @Positive(message = "Unit quantity must be positive")
    private Double unitQty;

    /**
     * Total calories (from YOLO CV service)
     */
    @NotNull(message = "Calories are required")
    @Min(value = 0, message = "Calories must be non-negative")
    private Integer kcal;

    /**
     * Image URL from OSS
     */
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
}

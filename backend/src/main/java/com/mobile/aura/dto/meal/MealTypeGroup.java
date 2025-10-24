package com.mobile.aura.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for meals grouped by meal type.
 * Represents a single meal type category with its meals and subtotal.
 */
@Data
@Builder
@AllArgsConstructor
public class MealTypeGroup {

    /**
     * Meal type code: 0=breakfast, 1=lunch, 2=dinner, 3=snack
     */
    private Integer mealType;

    /**
     * Display name for the meal type (e.g., "Breakfast", "Lunch")
     */
    private String mealTypeName;

    /**
     * Subtotal calories for this meal type
     */
    private Integer subtotalKcal;

    /**
     * List of meal items for this meal type
     */
    private List<MealItemResp> meals;
}

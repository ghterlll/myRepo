package com.mobile.aura.dto.meal;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response DTO for daily meal summary.
 * Contains meals grouped by meal type (breakfast, lunch, dinner, snack).
 */
@Data
@Builder
public class DailySummaryResp {

    /**
     * Date in format "yyyy-MM-dd"
     */
    private String date;

    /**
     * Total calories consumed for the day
     */
    private Integer totalCalories;

    /**
     * Meals grouped by meal type
     * Each group contains the meal type, display name, subtotal, and meal items
     */
    private List<MealTypeGroup> mealsByType;

    /**
     * Flat list of all meal items for frontend compatibility
     * This is the main field that frontend expects
     */
    private List<MealItemResp> items;
}
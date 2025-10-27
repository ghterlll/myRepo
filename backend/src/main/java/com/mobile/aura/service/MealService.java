package com.mobile.aura.service;

import com.mobile.aura.dto.meal.DailySummaryResp;
import com.mobile.aura.dto.meal.MealAddFreeInputReq;
import com.mobile.aura.dto.meal.MealAddFromSourceReq;
import com.mobile.aura.dto.meal.MealEditReq;

import java.time.LocalDate;

/**
 * Service interface for meal logging operations.
 * Handles adding, editing, deleting, and summarizing meal records.
 */
public interface MealService {
    /**
     * Add meal from existing food source
     * @param userId user ID
     * @param req request with sourceId
     * @return meal log ID
     */
    Long addFromSource(Long userId, MealAddFromSourceReq req);

    /**
     * Add meal with free input (no food source)
     * @param userId user ID
     * @param req request with all meal details
     * @return meal log ID
     */
    Long addFreeInput(Long userId, MealAddFreeInputReq req);

    /**
     * Edit an existing meal log.
     * Only updates fields that are provided in the request.
     *
     * @param userId user ID (for authorization)
     * @param mealId meal log ID to edit
     * @param req request with optional fields to update
     */
    void edit(Long userId, Long mealId, MealEditReq req);

    /**
     * Delete a meal log.
     * Performs soft delete by marking the meal as deleted.
     *
     * @param userId user ID (for authorization)
     * @param mealId meal log ID to delete
     */
    void delete(Long userId, Long mealId);

    /**
     * Get daily summary for a user on a specific date.
     * Returns aggregated meal data including total calories, protein, carbs, fat,
     * and individual meal records.
     *
     * @param userId user ID
     * @param date date to get summary for (null defaults to today)
     * @return daily summary response
     */
    DailySummaryResp daySummary(Long userId, LocalDate date);
}

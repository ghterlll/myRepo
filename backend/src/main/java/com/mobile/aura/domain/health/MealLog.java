package com.mobile.aura.domain.health;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.constant.MealType;
import com.mobile.aura.domain.aggregate.UserFoodItem;
import com.mobile.aura.dto.meal.*;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Data
@TableName("meal_log")
public class MealLog {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TableId
    private Long id;
    private Long userId;
    private LocalDate mealDate;
    private Integer mealType;
    private Long sourceId;  // User food item ID (NULL for free input)
    private String itemName;
    private String unitName;
    private Double unitQty;
    private Integer kcal;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    /**
     * Factory method to create meal log from food source.
     * Fetches all food details from UserFoodItem.
     * Meal date defaults to today if not provided.
     *
     * @param userId user ID
     * @param request request DTO with meal details and optional date
     * @param userFoodItem food item from database
     * @return new meal log instance
     */
    public static MealLog createFromSource(Long userId, MealAddFromSourceReq request, UserFoodItem userFoodItem) {
        validateUserFoodItem(userFoodItem, userId);

        MealLog mealLog = new MealLog();
        mealLog.userId = userId;
        mealLog.mealDate = request.getDateAsLocalDate();
        mealLog.mealType = request.getMealType();
        mealLog.sourceId = request.getSourceId();
        mealLog.itemName = userFoodItem.getName();
        mealLog.unitName = userFoodItem.getUnitName();
        mealLog.unitQty = getQuantityOrDefault(request.getUnitQty());
        mealLog.kcal = calculateCalories(userFoodItem.getKcalPerUnit(), mealLog.unitQty);
        mealLog.imageUrl = userFoodItem.getImageUrl();
        mealLog.createdAt = LocalDateTime.now();
        return mealLog;
    }

    /**
     * Factory method to create meal log from free input.
     * All meal details provided by client.
     * Meal date defaults to today if not provided.
     *
     * @param userId user ID
     * @param request request DTO with all meal details and optional date
     * @return new meal log instance
     */
    public static MealLog createFromFreeInput(Long userId, MealAddFreeInputReq request) {
        MealLog mealLog = new MealLog();
        mealLog.userId = userId;
        mealLog.mealDate = request.getDateAsLocalDate();
        mealLog.mealType = request.getMealType();
        mealLog.sourceId = null;
        mealLog.itemName = request.getItemName().trim();
        mealLog.unitName = request.getUnitName() != null ? request.getUnitName().trim() : "unit";
        mealLog.unitQty = getQuantityOrDefault(request.getUnitQty());
        mealLog.kcal = request.getKcal();
        mealLog.imageUrl = request.getImageUrl();
        mealLog.createdAt = LocalDateTime.now();
        return mealLog;
    }

    // Business method to map to response DTO
    public MealItemResp toMealItemResp() {
        return new MealItemResp(
            this.id,
            this.mealType,
            this.itemName,
            this.unitName,
            this.unitQty,
            this.kcal,
            this.imageUrl,
            this.createdAt == null ? null : TIME_FORMATTER.format(this.createdAt)
        );
    }

    // Business method for soft delete
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    // Business query methods
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public boolean belongsToUser(Long userId) {
        return Objects.equals(this.userId, userId);
    }

    /**
     * Validates that this meal log is accessible by the given user for modification/deletion.
     * Throws BizException if the meal log is null, deleted, or doesn't belong to the user.
     *
     * @param userId The user attempting to access this meal log
     * @throws BizException if validation fails
     */
    public void ensureAccessibleBy(Long userId) {
        if (this.isDeleted() || !this.belongsToUser(userId)) {
            throw new BizException(CommonStatusEnum.MEAL_LOG_NOT_FOUND);
        }
    }

    /**
     * Updates meal log fields from edit request.
     * Only updates fields that are provided (non-null).
     * For sourceId-based meals, recalculates calories if quantity changes.
     *
     * @param request edit request with optional fields
     * @param userFoodItem food item from database (null if this is free input meal)
     */
    public void updateFromRequest(com.mobile.aura.dto.meal.MealEditReq request, UserFoodItem userFoodItem) {
        // Update date if provided
        if (request.getDate() != null) {
            this.mealDate = request.getDateAsLocalDate();
        }

        // Update meal type if provided
        if (request.getMealType() != null) {
            this.mealType = request.getMealType();
        }

        // Update item name if provided
        if (request.getItemName() != null && !request.getItemName().isBlank()) {
            this.itemName = request.getItemName().trim();
        }

        // Update unit name if provided
        if (request.getUnitName() != null && !request.getUnitName().isBlank()) {
            this.unitName = request.getUnitName().trim();
        }

        // Update quantity and recalculate calories if needed
        if (request.getUnitQty() != null) {
            this.unitQty = request.getUnitQty();

            // If this meal has a source, recalculate calories based on new quantity
            if (this.sourceId != null && userFoodItem != null) {
                this.kcal = calculateCalories(userFoodItem.getKcalPerUnit(), this.unitQty);
            }
        }

        // Update calories directly if provided (for free input meals or manual override)
        if (request.getKcal() != null) {
            this.kcal = request.getKcal();
        }

        // Update image URL if provided
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            this.imageUrl = request.getImageUrl();
        }
    }

    // Business rule validation methods
    private static void validateUserFoodItem(UserFoodItem userFoodItem, Long userId) {
        if (userFoodItem == null || !Objects.equals(userFoodItem.getUserId(), userId)) {
            throw new BizException(CommonStatusEnum.FOOD_NOT_FOUND);
        }
    }

    // Helper methods
    private static double getQuantityOrDefault(Double quantity) {
        return quantity == null ? 1.0 : quantity;
    }

    private static int calculateCalories(Integer kcalPerUnit, double quantity) {
        return (int) Math.round(kcalPerUnit * quantity);
    }

    /**
     * Domain service: Build daily summary from meal logs.
     * Groups meals by meal type and calculates totals and subtotals.
     * This is pure business logic that belongs in the domain layer.
     *
     * @param mealLogs list of meal logs for a specific date
     * @param date the date string (yyyy-MM-dd)
     * @return DailySummaryResp with grouped meals
     */
    public static DailySummaryResp buildDailySummary(List<MealLog> mealLogs, String date) {
        // Calculate total calories
        int totalCalories = mealLogs.stream()
                .mapToInt(MealLog::getKcal)
                .sum();

        // Group meals by meal type
        Map<Integer, List<MealLog>> mealsByType = mealLogs.stream()
                .collect(Collectors.groupingBy(MealLog::getMealType));

        // Build meal type groups in order (Breakfast, Lunch, Dinner, Snack)
        List<MealTypeGroup> mealTypeGroups = new ArrayList<>();
        for (int mealType = MealType.BREAKFAST; mealType <= MealType.SNACK; mealType++) {
            List<MealLog> mealsForType = mealsByType.getOrDefault(mealType, Collections.emptyList());

            // Only include meal types that have meals
            if (!mealsForType.isEmpty()) {
                // Calculate subtotal for this meal type
                int subtotalKcal = mealsForType.stream()
                        .mapToInt(MealLog::getKcal)
                        .sum();

                // Convert to response DTOs
                List<MealItemResp> mealItems = mealsForType.stream()
                        .map(MealLog::toMealItemResp)
                        .toList();

                mealTypeGroups.add(MealTypeGroup.builder()
                        .mealType(mealType)
                        .mealTypeName(MealType.getDisplayName(mealType))
                        .subtotalKcal(subtotalKcal)
                        .meals(mealItems)
                        .build());
            }
        }

        // Create flat list of all meal items for frontend compatibility
        List<MealItemResp> allItems = mealLogs.stream()
                .map(MealLog::toMealItemResp)
                .toList();

        return DailySummaryResp.builder()
                .date(date)
                .totalCalories(totalCalories)
                .mealsByType(mealTypeGroups)
                .items(allItems)
                .build();
    }

    /**
     * Domain service: Calculate healthy days count.
     * A day is considered "healthy" if total calories fall within the target range.
     *
     * @param allMealLogs all meal logs for the user
     * @param targetCalories daily target calories
     * @param lowerBound lower bound multiplier (e.g., 0.9 for 90%)
     * @param upperBound upper bound multiplier (e.g., 1.1 for 110%)
     * @return number of healthy days
     */
    public static long countHealthyDays(List<MealLog> allMealLogs, int targetCalories,
                                       double lowerBound, double upperBound) {
        // Group by date and sum calories
        Map<LocalDate, Integer> dailyCaloriesByDate = allMealLogs.stream()
                .collect(Collectors.groupingBy(
                        MealLog::getMealDate,
                        Collectors.summingInt(MealLog::getKcal)
                ));

        // Count days within healthy range
        double lowerLimit = lowerBound * targetCalories;
        double upperLimit = upperBound * targetCalories;

        return dailyCaloriesByDate.values().stream()
                .filter(dailyCalories -> dailyCalories >= lowerLimit && dailyCalories <= upperLimit)
                .count();
    }
}

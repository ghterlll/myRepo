package com.mobile.aura.constant;

/**
 * Meal type constants.
 * Represents different types of meals throughout the day.
 */
public final class MealType {
    private MealType(){}

    public static final int BREAKFAST = 0;
    public static final int LUNCH     = 1;
    public static final int DINNER    = 2;
    public static final int SNACK     = 3;

    /**
     * Validate if meal type code is valid.
     *
     * @param t meal type code
     * @return true if valid (0-3), false otherwise
     */
    public static boolean valid(Integer t){
        return t!=null && t>=0 && t<=3;
    }

    /**
     * Get display name by meal type code.
     *
     * @param mealType meal type code (0-3)
     * @return display name
     */
    public static String getDisplayName(int mealType) {
        return switch (mealType) {
            case BREAKFAST -> "Breakfast";
            case LUNCH -> "Lunch";
            case DINNER -> "Dinner";
            case SNACK -> "Snack";
            default -> "Unknown";
        };
    }
}

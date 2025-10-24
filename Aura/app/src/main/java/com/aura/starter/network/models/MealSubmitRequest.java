package com.aura.starter.network.models;

// Minimal request body for POST /meal per backend contract
public class MealSubmitRequest {
    private String date;      // yyyy-MM-dd
    private Integer mealType; // 0 breakfast,1 lunch,2 dinner,3 snack
    private Long foodItemId;  // backend food library id (or user-food-item id)
    private Double servings;  // number of units

    public MealSubmitRequest(String date, Integer mealType, Long foodItemId, Double servings) {
        this.date = date;
        this.mealType = mealType;
        this.foodItemId = foodItemId;
        this.servings = servings;
    }

    public String getDate() { return date; }
    public Integer getMealType() { return mealType; }
    public Long getFoodItemId() { return foodItemId; }
    public Double getServings() { return servings; }
}



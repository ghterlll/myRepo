package com.aura.starter.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Legacy food record model (no longer used with Room DB)
 * Kept for compatibility with old code
 */
public class FoodRecord {
    public int id;
    
    public String mealType; // Breakfast, Lunch, Dinner, Snack
    public String foodName;
    public int calories;
    public int quantity; // Amount consumed
    public boolean isUsingGrams; // true for grams, false for servings
    public String date; // YYYY-MM-DD format
    public String time; // HH:mm format
    public long timestamp; // Unix timestamp for sorting
    public long sessionId; // Unique ID for each recording session
    
    public FoodRecord() {
        // Default constructor
    }
    
    public FoodRecord(String mealType, String foodName, int calories, int quantity, boolean isUsingGrams, long sessionId) {
        this.mealType = mealType;
        this.foodName = foodName;
        this.calories = calories;
        this.quantity = quantity;
        this.isUsingGrams = isUsingGrams;
        this.sessionId = sessionId;
        
        // Set current date and time
        Date now = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        this.date = dateFormat.format(now);
        this.time = timeFormat.format(now);
        this.timestamp = now.getTime();
    }
    
    // Getter and setter methods
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }
    
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    
    public int getCalories() { return calories; }
    public void setCalories(int calories) { this.calories = calories; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    
    public boolean isUsingGrams() { return isUsingGrams; }
    public void setUsingGrams(boolean usingGrams) { isUsingGrams = usingGrams; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    // Helper method to get formatted quantity string
    public String getFormattedQuantity() {
        if (isUsingGrams) {
            return quantity + "g";
        } else {
            return quantity + " serving" + (quantity > 1 ? "s" : "");
        }
    }
}

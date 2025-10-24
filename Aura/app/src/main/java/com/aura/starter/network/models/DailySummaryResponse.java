package com.aura.starter.network.models;

import com.google.gson.annotations.SerializedName;
import java.math.BigDecimal;
import java.util.List;

public class DailySummaryResponse {
    private String date;
    @SerializedName(value = "totalKcal", alternate = {"totalCalories"})
    private Integer totalKcal;
    private Double totalCarbs;
    private Double totalProtein;
    private Double totalFat;
    @SerializedName(value = "items", alternate = {"meals"})
    private List<MealItemDto> items;

    public static class MealItemDto {
        private Long id;
        private int mealType;
        private String itemName;
        private String unitName;
        private BigDecimal unitQty;
        private int kcal;
        private BigDecimal carbsG;
        private BigDecimal proteinG;
        private BigDecimal fatG;
        private String createdAt;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public int getMealType() { return mealType; }
        public void setMealType(int mealType) { this.mealType = mealType; }
        
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        
        public String getUnitName() { return unitName; }
        public void setUnitName(String unitName) { this.unitName = unitName; }
        
        public BigDecimal getUnitQty() { return unitQty; }
        public void setUnitQty(BigDecimal unitQty) { this.unitQty = unitQty; }
        
        public int getKcal() { return kcal; }
        public void setKcal(int kcal) { this.kcal = kcal; }
        
        public BigDecimal getCarbsG() { return carbsG; }
        public void setCarbsG(BigDecimal carbsG) { this.carbsG = carbsG; }
        
        public BigDecimal getProteinG() { return proteinG; }
        public void setProteinG(BigDecimal proteinG) { this.proteinG = proteinG; }
        
        public BigDecimal getFatG() { return fatG; }
        public void setFatG(BigDecimal fatG) { this.fatG = fatG; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // Getters and setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public Integer getTotalKcal() { return totalKcal; }
    public void setTotalKcal(Integer totalKcal) { this.totalKcal = totalKcal; }
    
    public Double getTotalCarbs() { return totalCarbs; }
    public void setTotalCarbs(Double totalCarbs) { this.totalCarbs = totalCarbs; }
    
    public Double getTotalProtein() { return totalProtein; }
    public void setTotalProtein(Double totalProtein) { this.totalProtein = totalProtein; }
    
    public Double getTotalFat() { return totalFat; }
    public void setTotalFat(Double totalFat) { this.totalFat = totalFat; }
    
    public List<MealItemDto> getItems() { return items; }
    public void setItems(List<MealItemDto> items) { this.items = items; }
}


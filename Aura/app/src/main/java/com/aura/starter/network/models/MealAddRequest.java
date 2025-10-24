package com.aura.starter.network.models;

// Request model matching backend MealAddReq
public class MealAddRequest {
    private Integer mealType;       // 0/1/2/3 (breakfast/lunch/dinner/snack)
    private Integer sourceType;     // 0库 1自定义 2自由录入
    private Long sourceId;          // 当 sourceType=0/1 必填
    private String itemName;        // 自由录入时必填
    private String unitName;        // 自由录入时必填
    private Double unitQty;         // 本次吃了几单位
    private String date;            // "yyyy-MM-dd"，为空则默认今天

    public MealAddRequest() {}

    public MealAddRequest(Integer mealType, Integer sourceType, Long sourceId, String itemName, String unitName, Double unitQty, String date) {
        this.mealType = mealType;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.itemName = itemName;
        this.unitName = unitName;
        this.unitQty = unitQty;
        this.date = date;
    }

    // Getters and Setters
    public Integer getMealType() { return mealType; }
    public void setMealType(Integer mealType) { this.mealType = mealType; }

    public Integer getSourceType() { return sourceType; }
    public void setSourceType(Integer sourceType) { this.sourceType = sourceType; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }

    public Double getUnitQty() { return unitQty; }
    public void setUnitQty(Double unitQty) { this.unitQty = unitQty; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
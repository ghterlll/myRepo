package com.aura.starter.network.models;

public class ExerciseAddRequest {
    private String exerciseDate;  // yyyy-MM-dd
    private int sourceType;       // 0=system, 1=user_custom
    private Long sourceId;
    private String itemName;
    private int minutes;
    private int kcal;

    public String getExerciseDate() { return exerciseDate; }
    public void setExerciseDate(String exerciseDate) { this.exerciseDate = exerciseDate; }
    
    public int getSourceType() { return sourceType; }
    public void setSourceType(int sourceType) { this.sourceType = sourceType; }
    
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    
    public int getMinutes() { return minutes; }
    public void setMinutes(int minutes) { this.minutes = minutes; }
    
    public int getKcal() { return kcal; }
    public void setKcal(int kcal) { this.kcal = kcal; }
}


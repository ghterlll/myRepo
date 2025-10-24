package com.aura.starter.network.models;

import java.util.List;

public class DailyWorkoutResponse {
    private String exerciseDate;
    private int totalKcal;
    private int totalMinutes;
    private List<ExerciseItemDto> items;

    public static class ExerciseItemDto {
        private Long id;
        private String itemName;
        private int minutes;
        private int kcal;
        private String createdAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        
        public int getMinutes() { return minutes; }
        public void setMinutes(int minutes) { this.minutes = minutes; }
        
        public int getKcal() { return kcal; }
        public void setKcal(int kcal) { this.kcal = kcal; }
        
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public String getExerciseDate() { return exerciseDate; }
    public void setExerciseDate(String exerciseDate) { this.exerciseDate = exerciseDate; }
    
    public int getTotalKcal() { return totalKcal; }
    public void setTotalKcal(int totalKcal) { this.totalKcal = totalKcal; }
    
    public int getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(int totalMinutes) { this.totalMinutes = totalMinutes; }
    
    public List<ExerciseItemDto> getItems() { return items; }
    public void setItems(List<ExerciseItemDto> items) { this.items = items; }
}


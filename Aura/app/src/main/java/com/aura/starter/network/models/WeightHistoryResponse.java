package com.aura.starter.network.models;

import java.math.BigDecimal;
import java.util.List;

public class WeightHistoryResponse {
    private List<WeightDayItem> items;

    public static class WeightDayItem {
        private String date;
        private BigDecimal weightKg;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public BigDecimal getWeightKg() { return weightKg; }
        public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    }

    public List<WeightDayItem> getItems() { return items; }
    public void setItems(List<WeightDayItem> items) { this.items = items; }
    
    public List<WeightDayItem> getLogs() { return items; }
}


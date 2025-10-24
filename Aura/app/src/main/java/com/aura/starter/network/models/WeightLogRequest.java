package com.aura.starter.network.models;

import java.math.BigDecimal;

public class WeightLogRequest {
    private String date;  // yyyy-MM-dd
    private BigDecimal weightKg;
    private String note;

    public WeightLogRequest(String date, BigDecimal weightKg) {
        this.date = date;
        this.weightKg = weightKg;
    }
    
    public WeightLogRequest(String date, BigDecimal weightKg, String note) {
        this.date = date;
        this.weightKg = weightKg;
        this.note = note;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}


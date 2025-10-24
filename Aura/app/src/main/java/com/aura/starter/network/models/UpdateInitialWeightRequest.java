package com.aura.starter.network.models;

import java.math.BigDecimal;

public class UpdateInitialWeightRequest {
    private String date; // yyyy-MM-dd, nullable -> today
    private BigDecimal weightKg;

    public UpdateInitialWeightRequest(String date, BigDecimal weightKg) {
        this.date = date;
        this.weightKg = weightKg;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
}

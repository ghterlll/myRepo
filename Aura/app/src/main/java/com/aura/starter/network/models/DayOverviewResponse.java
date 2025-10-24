package com.aura.starter.network.models;

import java.math.BigDecimal;

public class DayOverviewResponse {
    private String date;
    
    // 饮食数据
    private Integer totalKcalIn;
    private BigDecimal totalCarbsG;
    private BigDecimal totalProteinG;
    private BigDecimal totalFatG;
    
    // 运动数据
    private Integer totalKcalOut;
    private Integer totalMinutes;
    
    // 饮水数据
    private Integer totalWaterMl;
    
    // 体重数据
    private BigDecimal weightKg;

    // Getters and setters
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public Integer getTotalKcalIn() { return totalKcalIn; }
    public void setTotalKcalIn(Integer totalKcalIn) { this.totalKcalIn = totalKcalIn; }
    
    public BigDecimal getTotalCarbsG() { return totalCarbsG; }
    public void setTotalCarbsG(BigDecimal totalCarbsG) { this.totalCarbsG = totalCarbsG; }
    
    public BigDecimal getTotalProteinG() { return totalProteinG; }
    public void setTotalProteinG(BigDecimal totalProteinG) { this.totalProteinG = totalProteinG; }
    
    public BigDecimal getTotalFatG() { return totalFatG; }
    public void setTotalFatG(BigDecimal totalFatG) { this.totalFatG = totalFatG; }
    
    public Integer getTotalKcalOut() { return totalKcalOut; }
    public void setTotalKcalOut(Integer totalKcalOut) { this.totalKcalOut = totalKcalOut; }
    
    public Integer getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(Integer totalMinutes) { this.totalMinutes = totalMinutes; }
    
    public Integer getTotalWaterMl() { return totalWaterMl; }
    public void setTotalWaterMl(Integer totalWaterMl) { this.totalWaterMl = totalWaterMl; }
    
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
}


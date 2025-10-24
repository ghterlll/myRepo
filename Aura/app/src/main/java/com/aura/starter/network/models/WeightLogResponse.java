package com.aura.starter.network.models;

import java.math.BigDecimal;

public class WeightLogResponse {
    private String latestDate;
    private BigDecimal latestWeightKg;
    private String initialDate;
    private BigDecimal initialWeightKg;
    private BigDecimal targetWeightKg;
    private String targetDate;

    public String getLatestDate() { return latestDate; }
    public void setLatestDate(String latestDate) { this.latestDate = latestDate; }
    
    public BigDecimal getLatestWeightKg() { return latestWeightKg; }
    public void setLatestWeightKg(BigDecimal latestWeightKg) { this.latestWeightKg = latestWeightKg; }
    
    public String getInitialDate() { return initialDate; }
    public void setInitialDate(String initialDate) { this.initialDate = initialDate; }
    
    public BigDecimal getInitialWeightKg() { return initialWeightKg; }
    public void setInitialWeightKg(BigDecimal initialWeightKg) { this.initialWeightKg = initialWeightKg; }
    
    public BigDecimal getTargetWeightKg() { return targetWeightKg; }
    public void setTargetWeightKg(BigDecimal targetWeightKg) { this.targetWeightKg = targetWeightKg; }
    
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
}


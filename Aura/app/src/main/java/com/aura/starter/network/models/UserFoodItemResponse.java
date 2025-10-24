package com.aura.starter.network.models;

import java.math.BigDecimal;

public class UserFoodItemResponse {
    private Long id;
    private String name;
    private String unitName;
    private int kcalPerUnit;
    private BigDecimal carbsG;
    private BigDecimal proteinG;
    private BigDecimal fatG;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    
    public int getKcalPerUnit() { return kcalPerUnit; }
    public void setKcalPerUnit(int kcalPerUnit) { this.kcalPerUnit = kcalPerUnit; }
    
    public BigDecimal getCarbsG() { return carbsG; }
    public void setCarbsG(BigDecimal carbsG) { this.carbsG = carbsG; }
    
    public BigDecimal getProteinG() { return proteinG; }
    public void setProteinG(BigDecimal proteinG) { this.proteinG = proteinG; }
    
    public BigDecimal getFatG() { return fatG; }
    public void setFatG(BigDecimal fatG) { this.fatG = fatG; }
}


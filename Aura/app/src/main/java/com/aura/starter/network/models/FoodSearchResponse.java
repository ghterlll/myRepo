package com.aura.starter.network.models;

import java.math.BigDecimal;
import java.util.List;

public class FoodSearchResponse {
    private List<FoodItemDto> items;

    public static class FoodItemDto {
        private Long id;
        private String name;
        private String alias;
        private String category;
        private String unitName;
        private int kcalPerUnit;
        private BigDecimal carbsG;
        private BigDecimal proteinG;
        private BigDecimal fatG;
        private String giLabel;

        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getAlias() { return alias; }
        public void setAlias(String alias) { this.alias = alias; }
        
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        
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
        
        public String getGiLabel() { return giLabel; }
        public void setGiLabel(String giLabel) { this.giLabel = giLabel; }
    }

    public List<FoodItemDto> getItems() { return items; }
    public void setItems(List<FoodItemDto> items) { this.items = items; }
}


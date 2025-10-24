package com.aura.starter.data;

/**
 * Legacy water record model (no longer used with Room DB)
 * Kept for compatibility with old code
 */
public class WaterRecord {
    public int id;
    
    public String drinkType;
    public int milliliters;
    public long timestamp;
    public String date; // YYYY-MM-DD format for easy querying
    
    public WaterRecord() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public WaterRecord(String drinkType, int milliliters, String date) {
        this.drinkType = drinkType;
        this.milliliters = milliliters;
        this.date = date;
        this.timestamp = System.currentTimeMillis();
    }
}

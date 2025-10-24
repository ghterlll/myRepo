package com.aura.starter.network.models;

public class WaterDailySummaryResponse {
    private String date;
    private int amountMl;

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public int getAmountMl() { return amountMl; }
    public void setAmountMl(int amountMl) { this.amountMl = amountMl; }
    
    public int getTotalMl() { return amountMl; }
}


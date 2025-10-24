package com.aura.starter.network.models;

public class WaterAddRequest {
    private String date;  // yyyy-MM-dd
    private int amountMl;

    public WaterAddRequest(String date, int amountMl) {
        this.date = date;
        this.amountMl = amountMl;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public int getAmountMl() { return amountMl; }
    public void setAmountMl(int amountMl) { this.amountMl = amountMl; }
}


package com.aura.starter.network.models;

import java.util.List;

public class WaterRangeResponse {
    private List<WaterDayItem> items;

    public static class WaterDayItem {
        private String date;
        private int amountMl;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public int getAmountMl() { return amountMl; }
        public void setAmountMl(int amountMl) { this.amountMl = amountMl; }
    }

    public List<WaterDayItem> getItems() { return items; }
    public void setItems(List<WaterDayItem> items) { this.items = items; }
}


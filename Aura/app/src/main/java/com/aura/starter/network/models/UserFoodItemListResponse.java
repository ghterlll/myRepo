package com.aura.starter.network.models;

import java.util.List;

public class UserFoodItemListResponse {
    private List<UserFoodItemResponse> items;

    public List<UserFoodItemResponse> getItems() { return items; }
    public void setItems(List<UserFoodItemResponse> items) { this.items = items; }
}


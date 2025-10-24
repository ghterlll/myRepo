package com.mobile.aura.dto.food;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FoodItemResp {
    private Long    id;
    private String  name;
    private String  alias;         // Frontend expects this (null for user items)
    private String  category;      // Frontend expects this (null for user items)
    private String  unitName;
    private Integer kcalPerUnit;
    private String  imageUrl;      // Food image URL from OSS (null for system items)
    private String  createdAt;     // yyyy-MM-dd HH:mm:ss, optional
}
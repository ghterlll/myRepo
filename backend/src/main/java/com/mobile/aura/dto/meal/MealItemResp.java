package com.mobile.aura.dto.meal;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Response DTO for a meal log item
 */
@Data
@AllArgsConstructor
public class MealItemResp {
    private Long id;
    private Integer mealType;    // 0=breakfast, 1=lunch, 2=dinner, 3=snack
    private String itemName;
    private String unitName;
    private Double unitQty;
    private Integer kcal;
    private String imageUrl;     // Food image URL from OSS
    private String createdAt;    // "yyyy-MM-dd HH:mm:ss"
}
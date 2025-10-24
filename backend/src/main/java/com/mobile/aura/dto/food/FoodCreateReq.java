package com.mobile.aura.dto.food;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a custom food item.
 * <p>
 * Flow:
 * 1. Client uploads image via POST /files/food/image -> gets imageUrl
 * 2. Client calls YOLO CV service to get kcalPerUnit
 * 3. Client sends this request with imageUrl and kcalPerUnit
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodCreateReq {

    /**
     * Food name
     * REQUIRED
     */
    @NotBlank(message = "Food name is required")
    private String name;

    /**
     * Unit of measurement
     * OPTIONAL - defaults to "unit" if not provided
     */
    private String unitName;

    /**
     * Calories per unit (from YOLO CV service)
     * REQUIRED
     */
    @NotNull(message = "Calories per unit is required")
    @Min(value = 0, message = "Calories must be non-negative")
    private Integer kcalPerUnit;

    /**
     * Image URL from OSS
     * REQUIRED
     */
    @NotBlank(message = "Image URL is required")
    private String imageUrl;
}
package com.mobile.aura.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Base class for paginated requests with cursor-based pagination.
 * Provides common fields and methods for handling pagination parameters.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * Maximum number of items to return per page.
     */
    @Min(value = 1, message = "Limit must be at least 1")
    @Max(value = 100, message = "Limit must not exceed 100")
    private Integer limit = 20;

    /**
     * Cursor for pagination (format: "timestamp|id").
     */
    private String cursor;


    /**
     * Get normalized page limit.
     *
     * @return PageLimit object with normalized value
     */
    public PageLimit getPageLimit() {
        return PageLimit.of(limit);
    }
}

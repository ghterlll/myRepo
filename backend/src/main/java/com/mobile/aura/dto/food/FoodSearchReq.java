package com.mobile.aura.dto.food;

import com.mobile.aura.dto.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Request DTO for searching user's custom food items.
 * System food library has been moved to frontend.
 * Extends PageRequest for cursor-based pagination support.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FoodSearchReq extends PageRequest {

    /**
     * Search query (optional).
     */
    private String q;

    /**
     * Parse cursor string to Long ID.
     * For Food module, cursor is just the ID (not timestamp|id format).
     *
     * @return Long cursor ID, or null if cursor is null/blank
     */
    public Long getCursorAsLong() {
        if (getCursor() == null || getCursor().isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(getCursor());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

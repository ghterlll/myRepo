package com.mobile.aura.dto;

import lombok.Getter;

/**
 * PageLimit value object for pagination.
 * Ensures limit is within valid bounds.
 */
@Getter
public class PageLimit {
    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 100;

    private final int value;

    private PageLimit(int value) {
        this.value = Math.max(MIN_LIMIT, Math.min(MAX_LIMIT, value));
    }

    /**
     * Create a PageLimit from a raw value.
     *
     * @param limit the requested limit
     * @return PageLimit object with normalized value
     */
    public static PageLimit of(int limit) {
        return new PageLimit(limit);
    }

}

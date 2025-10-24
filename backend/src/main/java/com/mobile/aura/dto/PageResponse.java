package com.mobile.aura.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Generic paginated response with cursor-based pagination metadata.
 * Provides consistent pagination response format across all APIs.
 * Rich domain model with pagination logic encapsulated.
 *
 * @param <T> the type of items in the list
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    /**
     * List of items for current page.
     */
    private List<T> items;

    /**
     * Cursor for next page (format: "timestamp|id").
     * Null if no more data available.
     */
    private String nextCursor;

    /**
     * Whether there are more items to fetch.
     * True if there's a next page, false otherwise.
     */
    private Boolean hasMore;

    /**
     * Build a PageResponse from fetched items with cursor extraction.
     * Automatically handles pagination logic: determines hasMore, extracts cursor.
     * <p>
     * Usage pattern:
     * 1. Query with limit+1 to detect if more data exists
     * 2. Call this method with the results
     *
     * @param items all fetched items (limit + 1)
     * @param limit the page size limit
     * @param cursorExtractor function to extract cursor string from last item
     * @param <T> the type of items
     * @return PageResponse with proper pagination metadata
     */
    public static <T> PageResponse<T> paginate(
            List<T> items,
            int limit,
            Function<T, String> cursorExtractor
    ) {
        boolean hasMore = items.size() > limit;
        List<T> resultItems = hasMore ? items.subList(0, limit) : items;

        String nextCursor = Optional.of(hasMore)
                .filter(more -> !resultItems.isEmpty())
                .map(more -> resultItems.getLast())
                .map(cursorExtractor)
                .orElse(null);

        return new PageResponse<>(resultItems, nextCursor, hasMore);
    }

    /**
     * Factory method to create a response when there's more data.
     *
     * @param items list of items
     * @param nextCursor cursor for next page
     * @param <T> item type
     * @return PageResponse with hasMore=true
     */
    public static <T> PageResponse<T> of(List<T> items, String nextCursor) {
        return new PageResponse<>(items, nextCursor, nextCursor != null);
    }

    /**
     * Factory method to create an empty response.
     *
     * @param <T> item type
     * @return PageResponse with empty list and hasMore=false
     */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), null, false);
    }
}

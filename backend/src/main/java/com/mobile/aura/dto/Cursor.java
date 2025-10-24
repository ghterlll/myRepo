package com.mobile.aura.dto;

import lombok.Getter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Cursor value object for pagination.
 * Immutable object that encapsulates cursor parsing and building logic.
 * Rich domain model for cursor-based pagination.
 */
@Getter
public class Cursor {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final LocalDateTime timestamp;
    private final Long id;

    private Cursor(LocalDateTime timestamp, Long id) {
        this.timestamp = timestamp;
        this.id = id;
    }

    /**
     * Parse cursor string into Cursor object.
     *
     * @param cursorStr the cursor string (format: "timestamp|id")
     * @return Cursor object, or empty cursor if string is null/blank
     */
    public static Cursor parse(String cursorStr) {
        return Optional.ofNullable(cursorStr)
                .filter(s -> !s.isBlank())
                .map(s -> s.split("\\|"))
                .filter(parts -> parts.length >= 2)
                .map(parts -> new Cursor(
                        LocalDateTime.parse(parts[0], FORMATTER),
                        Long.parseLong(parts[1])
                ))
                .orElse(empty());
    }

    /**
     * Build a cursor string from timestamp and ID.
     *
     * @param timestamp the timestamp
     * @param id the entity ID
     * @return cursor string in format "timestamp|id"
     */
    public static String build(LocalDateTime timestamp, Long id) {
        return timestamp.format(FORMATTER) + "|" + id;
    }

    /**
     * Build a cursor string from timestamp string and ID.
     *
     * @param timestamp the timestamp string (already formatted)
     * @param id the entity ID
     * @return cursor string in format "timestamp|id"
     */
    public static String build(String timestamp, Long id) {
        return timestamp + "|" + id;
    }

    /**
     * Create an empty cursor (for first page).
     *
     * @return empty Cursor
     */
    public static Cursor empty() {
        return new Cursor(null, null);
    }

    /**
     * Check if this cursor is empty.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return timestamp == null && id == null;
    }

    /**
     * Format timestamp for SQL queries.
     * Converts LocalDateTime to SQL-compatible string format.
     *
     * @return formatted timestamp string, or null if empty
     */
    public String formatTimestamp() {
        return Optional.ofNullable(timestamp)
                .map(ts -> ts.format(FORMATTER))
                .orElse(null);
    }
}

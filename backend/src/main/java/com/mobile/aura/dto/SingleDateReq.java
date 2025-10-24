package com.mobile.aura.dto;

import com.mobile.aura.validation.PastOrPresentDate;
import lombok.Data;

import java.time.LocalDate;

/**
 * Base class for request DTOs that contain a single date field.
 * Provides common date field with validation for requests that operate on a specific day.
 * Date defaults to today if not provided.
 */
@Data
public abstract class SingleDateReq {

    /**
     * Date in format "yyyy-MM-dd"
     * Optional - defaults to today if not provided
     * Must be today or in the past
     */
    @PastOrPresentDate
    private String date;

    /**
     * Parse date string to LocalDate, defaults to today if null or blank.
     * This is a value object method that encapsulates date parsing logic.
     *
     * @return parsed LocalDate or today
     */
    public LocalDate getDateAsLocalDate() {
        return parseDateOrToday(this.date);
    }

    /**
     * Static utility method to parse date string to LocalDate.
     * Defaults to today if null or blank.
     * Useful for cases where there's no DTO instance (e.g., @RequestParam).
     *
     * @param dateStr date string in format "yyyy-MM-dd"
     * @return parsed LocalDate or today
     */
    public static LocalDate parseDateOrToday(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return LocalDate.now();
        }
        return LocalDate.parse(dateStr);
    }
}

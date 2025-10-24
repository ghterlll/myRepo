package com.mobile.aura.dto;

import com.mobile.aura.validation.PastOrPresentDate;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Base class for request DTOs that contain a date range (from/to).
 * Provides common date range fields with validation for requests that query data across a period.
 * Both dates default to appropriate values if not provided (implementation-specific).
 */
@Data
public abstract class DateRangeReq {

    /**
     * Start date in format "yyyy-MM-dd"
     * Optional - defaults vary by implementation (typically 7 days ago)
     * Must not be a future date
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in format yyyy-MM-dd")
    @PastOrPresentDate
    private String from;

    /**
     * End date in format "yyyy-MM-dd"
     * Optional - defaults to today if not provided
     * Must not be a future date
     */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Date must be in format yyyy-MM-dd")
    @PastOrPresentDate
    private String to;
}

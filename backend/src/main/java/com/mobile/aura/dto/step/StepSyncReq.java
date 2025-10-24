package com.mobile.aura.dto.step;

import com.mobile.aura.validation.PastOrPresentDate;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for syncing step count.
 * Supports idempotent synchronization with sequence numbers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepSyncReq {

    /**
     * Step count
     * Must be between 0 and 100,000
     */
    @NotNull(message = "Steps is required")
    @Min(value = 0, message = "Steps must be non-negative")
    @Max(value = 100_000, message = "Steps must not exceed 100,000")
    private Integer steps;

    /**
     * Date of step count record in format "yyyy-MM-dd"
     * Optional - defaults to today
     * Must be today or in the past
     */
    @PastOrPresentDate
    private String date;

    /**
     * Sync sequence number (client timestamp in milliseconds)
     * Used to ensure data order and prevent old data from overwriting new data
     */
    @NotNull(message = "Sync sequence is required")
    private Long syncSequence;

    /**
     * Client's known server version (for optimistic locking)
     * Optional - used to detect conflicts in multi-device scenarios
     */
    private Integer knownVersion;

    /**
     * Data source identifier
     * Examples: "Sensor", "GoogleFit", "HuaweiHealth", "Manual"
     * Optional - defaults to "Sensor"
     */
    private String dataSource;

    /**
     * Client timestamp (milliseconds)
     * Optional - used for debugging and clock skew detection
     */
    private Long clientTimestamp;
}

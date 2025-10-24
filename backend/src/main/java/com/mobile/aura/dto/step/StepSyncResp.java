package com.mobile.aura.dto.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for step sync operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepSyncResp {

    /**
     * Date in format "yyyy-MM-dd"
     */
    private String date;

    /**
     * Final step count accepted by server
     */
    private Integer steps;

    /**
     * Server version number (for optimistic locking)
     */
    private Integer version;

    /**
     * Sync sequence number recorded by server
     */
    private Long syncSequence;

    /**
     * Sync status result
     * ACCEPTED: Data accepted and saved
     * REJECTED: Data rejected (older than existing)
     * CONFLICT: Version conflict detected (multi-device)
     */
    private SyncStatus status;

    /**
     * Status message
     */
    private String message;

    /**
     * Calculated distance in kilometers (formatted string)
     */
    private String distanceKm;

    /**
     * Calculated calories burned
     */
    private Integer kcal;

    /**
     * Calculated active minutes
     */
    private Integer activeMinutes;

    /**
     * Data source
     */
    private String dataSource;

    public enum SyncStatus {
        ACCEPTED,   // Data accepted
        REJECTED,   // Data rejected (old sequence)
        CONFLICT    // Version conflict
    }
}

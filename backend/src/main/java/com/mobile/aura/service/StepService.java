package com.mobile.aura.service;

import com.mobile.aura.dto.step.*;

/**
 * Step count service interface.
 * Handles step count synchronization and query operations.
 */
public interface StepService {

    /**
     * Sync step count (idempotent operation).
     * Accepts new data only if syncSequence is newer than existing.
     * Updates step count only if new steps are greater.
     *
     * @param userId user ID
     * @param req sync request containing steps, date, and syncSequence
     * @return sync response with status (ACCEPTED/REJECTED/CONFLICT)
     */
    StepSyncResp syncSteps(Long userId, StepSyncReq req);

    /**
     * Batch sync step counts.
     * Used for syncing historical data or compensating failed syncs.
     *
     * @param userId user ID
     * @param req batch sync request containing multiple items
     * @return batch sync response with success/failure counts
     */
    BatchSyncResp batchSync(Long userId, BatchSyncReq req);

    /**
     * Pull step count updates from server (bidirectional sync).
     * Returns records that are newer than the specified timestamp.
     *
     * @param userId user ID
     * @param since timestamp in milliseconds (optional, defaults to 30 days ago)
     * @return pull response containing updated records
     */
    PullStepsResp pullSteps(Long userId, Long since);

    /**
     * Get step count for a specific day.
     * Date defaults to server's today if not provided, but clients should always pass explicit date.
     *
     * @param userId user ID
     * @param date date string in format "yyyy-MM-dd" (optional but recommended)
     * @return step count for the specified day (0 if no record exists)
     */
    StepDayResp getDay(Long userId, String date);

    /**
     * Get step counts for a date range.
     * Missing dates are automatically filled with 0 steps.
     *
     * @param userId user ID
     * @param from start date string (optional, defaults to 7 days ago)
     * @param to end date string (optional, defaults to today)
     * @return range response with daily step counts and statistics
     */
    StepRangeResp getRange(Long userId, String from, String to);
}

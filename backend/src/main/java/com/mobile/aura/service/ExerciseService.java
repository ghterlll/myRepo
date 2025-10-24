package com.mobile.aura.service;

import com.mobile.aura.dto.exercise.ExerciseAddReq;
import com.mobile.aura.dto.exercise.DailyWorkoutResp;
import com.mobile.aura.dto.exercise.ExerciseRangeResp;

import java.time.LocalDate;

/**
 * Exercise service interface.
 * Simplified design for running activity tracking.
 */
public interface ExerciseService {

    /**
     * Add exercise log.
     *
     * @param userId user ID
     * @param req exercise add request
     * @return created exercise log ID
     */
    Long add(Long userId, ExerciseAddReq req);

    /**
     * Delete exercise log (soft delete).
     *
     * @param userId user ID
     * @param exerciseLogId exercise log ID to delete
     */
    void delete(Long userId, Long exerciseLogId);

    /**
     * Get daily workout summary.
     *
     * @param userId user ID
     * @param date target date (null = today)
     * @return daily workout response with exercise logs
     */
    DailyWorkoutResp day(Long userId, LocalDate date);

    /**
     * Get exercise logs for a date range.
     * Returns daily workout summaries with aggregate statistics.
     * Maximum range is 31 days.
     *
     * @param userId user ID
     * @param from start date (null = 7 days ago)
     * @param to end date (null = today)
     * @return range response with daily summaries and statistics
     * @throws com.mobile.aura.support.BizException if date range exceeds 31 days
     */
    ExerciseRangeResp getRange(Long userId, String from, String to);
}

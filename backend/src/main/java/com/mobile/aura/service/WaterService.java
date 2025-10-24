package com.mobile.aura.service;

import com.mobile.aura.dto.water.WaterConfigResp;
import com.mobile.aura.dto.water.WaterConfigUpdateReq;
import com.mobile.aura.dto.water.WaterDayResp;
import com.mobile.aura.dto.water.WaterRangeResp;

import java.time.LocalDate;

/**
 * Water intake service interface.
 * Handles water intake tracking operations.
 */
public interface WaterService {

    /**
     * Submit/overwrite daily water intake.
     * Creates a new record if none exists, updates if exists.
     *
     * @param userId user ID
     * @param date intake date
     * @param amountMl total water intake in milliliters
     */
    void submit(Long userId, LocalDate date, int amountMl);

    /**
     * Increment daily water intake.
     * Adds the specified amount to existing water intake for the day.
     * Creates a new record with the increment amount if none exists.
     *
     * @param userId user ID
     * @param date intake date
     * @param incrementMl amount to add in milliliters
     */
    void increment(Long userId, LocalDate date, int incrementMl);

    /**
     * Delete water intake record for a specific day.
     *
     * @param userId user ID
     * @param date intake date
     */
    void deleteDay(Long userId, LocalDate date);

    /**
     * Get water intake for a specific day.
     *
     * @param userId user ID
     * @param date intake date
     * @return water intake response (0 ml if no record exists)
     */
    WaterDayResp day(Long userId, LocalDate date);

    /**
     * Get water intake for a date range.
     * Missing dates are automatically filled with 0 ml.
     *
     * @param userId user ID
     * @param from start date string in format "yyyy-MM-dd" (optional, defaults to 7 days ago)
     * @param to end date string in format "yyyy-MM-dd" (optional, defaults to today)
     * @return range response with daily water intake records
     */
    WaterRangeResp range(Long userId, String from, String to);

    /**
     * Get water configuration including quick records, goal, and current intake.
     *
     * @param userId user ID
     * @param date date for current intake (optional, defaults to today)
     * @return water configuration response
     */
    WaterConfigResp getConfig(Long userId, LocalDate date);

    /**
     * Update water configuration (quick records and/or goal).
     *
     * @param userId user ID
     * @param req update request
     */
    void updateConfig(Long userId, WaterConfigUpdateReq req);
}

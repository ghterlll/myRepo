package com.mobile.aura.service;

import com.mobile.aura.dto.weight.UpdateInitialWeightReq;
import com.mobile.aura.dto.weight.WeightLatestResp;
import com.mobile.aura.dto.weight.WeightRangeResp;
import com.mobile.aura.dto.weight.WeightSubmitReq;

/**
 * Weight tracking service interface.
 * Handles weight record operations and profile updates.
 */
public interface WeightService {

    /**
     * Submit/overwrite daily weight record.
     * Creates a new record if none exists for the date, updates if exists.
     * Also updates user profile with initial and latest weight information.
     *
     * @param userId user ID
     * @param req weight submission request
     */
    void submit(Long userId, WeightSubmitReq req);

    /**
     * Get weight records within a date range.
     *
     * @param userId user ID
     * @param start start date string in format "yyyy-MM-dd" (optional, defaults to today)
     * @param end end date string in format "yyyy-MM-dd" (optional, defaults to today)
     * @return weight range response with records
     */
    WeightRangeResp range(Long userId, String start, String end);

    /**
     * Get latest, initial, and target weight information.
     *
     * @param userId user ID
     * @return weight summary response
     */
    WeightLatestResp latest(Long userId);

    /**
     * Update initial weight information.
     * Allows users to correct their initial weight if they made a mistake.
     *
     * @param userId user ID
     * @param req update initial weight request
     */
    void updateInitialWeight(Long userId, UpdateInitialWeightReq req);
}

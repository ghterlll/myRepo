package com.mobile.aura.service;

/**
 * Service for automatically calculating and updating user activity levels
 * based on actual step count and exercise data.
 */
public interface ActivityLevelService {

    /**
     * Recalculate and update user's activity level based on recent activity data.
     * Uses the last 30 days of step counts and exercise logs.
     *
     * @param userId user ID
     */
    void recalculateAndUpdate(Long userId);
}

package com.mobile.aura.service;

import com.mobile.aura.dto.user.UserAggregateResp;
import com.mobile.aura.dto.user.UserBasicInfoResp;
import com.mobile.aura.dto.user.UserHealthProfileResp;
import com.mobile.aura.dto.user.UserProfileResp;
import com.mobile.aura.dto.user.UserProfileUpdateReq;

import java.util.List;

/**
 * Service interface for managing user profile operations.
 * Provides methods to retrieve and update user profile information,
 * including basic profile data, health profile data, and aggregated views.
 */
public interface UserProfileService {
    /**
     * Get complete user aggregate information.
     * Returns combined data from User, UserProfile, and UserHealthProfile.
     * This is a comprehensive view that includes basic user info,
     * biographical data, and health-related information.
     *
     * @param userId user ID to fetch aggregate data for
     * @return complete user aggregate response containing all profile data
     * @throws com.mobile.aura.support.BizException if user does not exist
     */
    UserAggregateResp getAggregate(Long userId);

    /**
     * Update user profile information with partial updates (PATCH semantics).
     * Only updates the fields provided in the request; null fields are ignored.
     * Creates a new profile if one doesn't exist (upsert behavior).
     * Can update both basic user info (nickname, avatar) and profile fields
     * (bio, gender, birthday, age, location, interests).
     *
     * @param userId user ID to update profile for
     * @param req request containing fields to update (null fields are ignored)
     * @throws com.mobile.aura.support.BizException if user does not exist
     */
    void updateProfile(Long userId, UserProfileUpdateReq req);

    /**
     * Get user's basic profile information only.
     * Returns biographical and personal information including bio, gender,
     * birthday, age, location, and interests. Does not include health data
     * or basic user info (use getAggregate for a complete view).
     *
     * @param userId user ID to fetch profile for
     * @return user profile response containing basic profile fields,
     *         returns empty response if profile doesn't exist
     * @throws com.mobile.aura.support.BizException if user does not exist
     */
    UserProfileResp getProfile(Long userId);

    /**
     * Get user's health profile information only.
     * Returns weight tracking, fitness goals, and health-related data including
     * height, initial/latest/target weight, activity level, diet rule, and
     * water intake goal. Does not include basic profile or user info.
     *
     * @param userId user ID to fetch health profile for
     * @return user health profile response containing health-related fields,
     *         returns empty response if health profile doesn't exist
     * @throws com.mobile.aura.support.BizException if user does not exist
     */
    UserHealthProfileResp getHealthProfile(Long userId);

    /**
     * Get basic user information for a list of user IDs in batch.
     * Returns only essential public info: user ID, nickname, and avatar.
     * Designed for efficient batch queries and list displays (e.g., followers list).
     * This method performs a single database query for all users, making it
     * significantly more efficient than individual queries.
     *
     * @param userIds list of user IDs to fetch basic info for
     * @return list of basic user info responses, only includes users that exist
     *         (non-existent user IDs are silently skipped)
     */
    List<UserBasicInfoResp> getBasicInfoBatch(List<Long> userIds);
}
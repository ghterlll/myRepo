package com.mobile.aura.service;

import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.user.UserBasicInfoResp;

/**
 * User relationship management service.
 * Handles following and blocking for user relationships.
 */
public interface RelationService {

    // ========== Follow/Unfollow Operations ==========

    /**
     * Follow a user.
     *
     * @param me the user ID performing the follow action
     * @param target the user ID to be followed
     * @throws com.mobile.aura.support.BizException if already following, blocked, or other business rules violated
     */
    void follow(Long me, Long target);

    /**
     * Unfollow a user.
     *
     * @param me the user ID performing the unfollow action
     * @param target the user ID to be unfollowed
     * @throws com.mobile.aura.support.BizException if not currently following
     */
    void unfollow(Long me, Long target);

    // ========== Block/Unblock Operations ==========

    /**
     * Block a user.
     * Blocks the target user and removes mutual following relationships.
     *
     * @param me the user ID performing the block action
     * @param target the user ID to be blocked
     */
    void block(Long me, Long target);

    /**
     * Unblock a user.
     *
     * @param me the user ID performing the unblock action
     * @param target the user ID to be unblocked
     */
    void unblock(Long me, Long target);

    // ========== Relationship Status Queries ==========

    /**
     * Check if one user is following another.
     *
     * @param me the follower user ID
     * @param target the followee user ID
     * @return true if following, false otherwise
     */
    boolean isFollowing(Long me, Long target);

    /**
     * Check if one user has blocked another.
     *
     * @param me the blocker user ID
     * @param target the blocked user ID
     * @return true if blocked, false otherwise
     */
    boolean isBlocked(Long me, Long target);

    /**
     * Check if one user is blocked by another.
     *
     * @param me the user ID checking block status
     * @param target the potential blocker user ID
     * @return true if blocked by target, false otherwise
     */
    boolean isBlockedBy(Long me, Long target);

    // ========== Relationship Lists ==========

    /**
     * Get followers list with basic user info.
     * Returns paginated list of users who follow the owner.
     * Filters out blocked users if viewer is specified.
     *
     * @param owner the user whose followers to retrieve
     * @param viewer the user viewing the list (null for guest/unauthenticated)
     * @param limit maximum number of results (1-100)
     * @param cursor pagination cursor (format: "createdAt|userId")
     * @return paginated response with follower basic info
     */
    PageResponse<UserBasicInfoResp> followers(Long owner, Long viewer, int limit, String cursor);

    /**
     * Get followings list with basic user info.
     * Returns paginated list of users that the owner follows.
     * Filters out blocked users if viewer is specified.
     *
     * @param owner the user whose followings to retrieve
     * @param viewer the user viewing the list (null for guest/unauthenticated)
     * @param limit maximum number of results (1-100)
     * @param cursor pagination cursor (format: "createdAt|userId")
     * @return paginated response with following basic info
     */
    PageResponse<UserBasicInfoResp> followings(Long owner, Long viewer, int limit, String cursor);

    /**
     * Get blocked users list with basic user info.
     * Returns the list of users that the current user has blocked.
     *
     * @param me the user whose block list to retrieve
     * @param limit maximum number of results (1-100)
     * @param cursor pagination cursor (format: "createdAt|userId")
     * @return paginated response with blocked user basic info
     */
    PageResponse<UserBasicInfoResp> blocks(Long me, int limit, String cursor);
}


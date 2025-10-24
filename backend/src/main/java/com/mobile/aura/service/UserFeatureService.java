package com.mobile.aura.service;


public interface UserFeatureService {
    /**
     * Create a snapshot of user features at a specific time
     * @param userId User ID
     * @return UserFeature ID
     */
    Long createSnapshot(Long userId);
}

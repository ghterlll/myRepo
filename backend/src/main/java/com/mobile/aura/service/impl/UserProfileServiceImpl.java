package com.mobile.aura.service.impl;

import com.mobile.aura.domain.user.User;
import com.mobile.aura.domain.user.UserHealthProfile;
import com.mobile.aura.domain.user.UserProfile;
import com.mobile.aura.dto.user.UserAggregateResp;
import com.mobile.aura.dto.user.UserBasicInfoResp;
import com.mobile.aura.dto.user.UserHealthProfileResp;
import com.mobile.aura.dto.user.UserProfileResp;
import com.mobile.aura.dto.user.UserProfileUpdateReq;
import com.mobile.aura.mapper.UserHealthProfileMapper;
import com.mobile.aura.mapper.UserMapper;
import com.mobile.aura.mapper.UserProfileMapper;
import com.mobile.aura.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Thin orchestration layer for user profile operations.
 * All business logic delegated to rich domain models.
 */
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserMapper userMapper;
    private final UserProfileMapper profileMapper;
    private final UserHealthProfileMapper healthProfileMapper;

    /**
     * Get user aggregate: basic user info + profile data + health data.
     */
    @Override
    public UserAggregateResp getAggregate(Long userId) {
        User user = User.ensureExists(userMapper.selectById(userId));
        UserProfile profile = profileMapper.selectById(userId);
        UserHealthProfile healthProfile = healthProfileMapper.findByUserId(userId);

        return UserAggregateResp.from(user, profile, healthProfile);
    }

    /**
     * Get user's basic profile information only.
     */
    @Override
    @Transactional(readOnly = true)
    public UserProfileResp getProfile(Long userId) {
        User.ensureExists(userMapper.selectById(userId));
        UserProfile profile = profileMapper.selectById(userId);
        return UserProfileResp.from(profile);
    }

    /**
     * Get user's health profile information only.
     */
    @Override
    @Transactional(readOnly = true)
    public UserHealthProfileResp getHealthProfile(Long userId) {
        User.ensureExists(userMapper.selectById(userId));
        UserHealthProfile healthProfile = healthProfileMapper.findByUserId(userId);
        return UserHealthProfileResp.from(healthProfile);
    }

    /**
     * PATCH update: only updates provided fields; creates profile if missing (upsert)
     */
    @Transactional
    @Override
    public void updateProfile(Long userId, UserProfileUpdateReq request) {
        updateUserBasicInfo(userId, request);
        upsertUserProfile(userId, request);
        upsertHealthProfile(userId, request);
    }

    /**
     * Update user basic info (nickname, avatar) if provided in request
     */
    private void updateUserBasicInfo(Long userId, UserProfileUpdateReq request) {
        if (request.getNickname() != null || request.getAvatarUrl() != null) {
            User user = User.ensureExists(userMapper.selectById(userId));
            user.updateNicknameIfProvided(request.getNickname());
            user.updateAvatarUrlIfProvided(request.getAvatarUrl());
            userMapper.updateById(user);
        }
    }

    /**
     * Upsert user profile: create if missing, update if exists
     */
    private void upsertUserProfile(Long userId, UserProfileUpdateReq request) {
        ProfileUpsertStrategy strategy = Optional.ofNullable(profileMapper.selectById(userId))
                .map(existing -> new ProfileUpsertStrategy(existing, profileMapper::updateById))
                .orElseGet(() -> new ProfileUpsertStrategy(UserProfile.createForUser(userId), profileMapper::insert));

        strategy.applyUpdatesAndSave(request);
    }

    /**
     * Upsert user health profile: create if missing, update if exists
     */
    private void upsertHealthProfile(Long userId, UserProfileUpdateReq request) {
        // Check if any health-related field is present in request
        if (!hasHealthProfileFields(request)) {
            return; // No health fields to update
        }

        HealthProfileUpsertStrategy strategy = Optional.ofNullable(healthProfileMapper.findByUserId(userId))
                .map(existing -> new HealthProfileUpsertStrategy(existing, healthProfileMapper::updateById))
                .orElseGet(() -> new HealthProfileUpsertStrategy(
                        UserHealthProfile.builder().userId(userId).build(),
                        healthProfileMapper::insert
                ));

        strategy.applyUpdatesAndSave(request);
    }

    /**
     * Check if request contains any health profile fields.
     * Note: activityLvl is excluded as it's auto-calculated, not user-editable.
     */
    private boolean hasHealthProfileFields(UserProfileUpdateReq request) {
        return request.getHeightCm() != null
                // activityLvl is intentionally excluded - it's auto-calculated
                || request.getTargetWeightKg() != null
                || request.getTargetDeadline() != null
                || request.getInitialWeightKg() != null
                || request.getInitialWeightAt() != null
                || request.getLatestWeightKg() != null
                || request.getLatestWeightAt() != null;
    }

    /**
     * Get basic user information for a batch of user IDs.
     * Efficiently fetches users in bulk, then maps to response DTOs.
     * Now only requires a single table query since avatarUrl is in User table.
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserBasicInfoResp> getBasicInfoBatch(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        // Batch fetch users - single query
        List<User> users = userMapper.selectBatchIds(userIds);
        if (users.isEmpty()) {
            return List.of();
        }

        // Map users to response DTOs
        return users.stream()
                .map(UserBasicInfoResp::from)
                .collect(Collectors.toList());
    }

    /**
     * Strategy pattern to eliminate if statements for upsert logic
     */
    private record ProfileUpsertStrategy(
            UserProfile profile,
            Consumer<UserProfile> persistenceAction
    ) {
        void applyUpdatesAndSave(UserProfileUpdateReq request) {
            profile.applyUpdates(request);
            persistenceAction.accept(profile);
        }
    }

    /**
     * Strategy pattern for health profile upsert logic
     */
    private record HealthProfileUpsertStrategy(
            UserHealthProfile healthProfile,
            Consumer<UserHealthProfile> persistenceAction
    ) {
        void applyUpdatesAndSave(UserProfileUpdateReq request) {
            healthProfile.applyUpdates(request);
            persistenceAction.accept(healthProfile);
        }
    }
}

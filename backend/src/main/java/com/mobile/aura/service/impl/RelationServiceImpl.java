package com.mobile.aura.service.impl;

import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.domain.user.UserBlock;
import com.mobile.aura.domain.user.UserFollow;
import com.mobile.aura.domain.user.UserSocialStats;
import com.mobile.aura.dto.Cursor;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.user.UserBasicInfoResp;
import com.mobile.aura.mapper.UserBlockMapper;
import com.mobile.aura.mapper.UserFollowMapper;
import com.mobile.aura.mapper.UserMapper;
import com.mobile.aura.mapper.UserSocialStatsMapper;
import com.mobile.aura.service.RelationService;
import com.mobile.aura.service.UserProfileService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of RelationService following Rich Domain Model principles.
 * No if statements, no setters, no builders in service layer.
 * All business logic delegated to rich domain models.
 * Uses PageResponse and Cursor for consistent pagination.
 */
@Service
@RequiredArgsConstructor
public class RelationServiceImpl implements RelationService {

    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final UserBlockMapper blockMapper;
    private final UserSocialStatsMapper socialStatsMapper;
    private final UserProfileService userProfileService;

    // === Helper methods ===

    @Override
    public boolean isFollowing(Long me, Long target) {
        return followMapper.countByFollowerAndFollowee(me, target) > 0;
    }

    @Override
    public boolean isBlocked(Long me, Long target) {
        return blockMapper.countBlock(me, target) > 0;
    }

    @Override
    public boolean isBlockedBy(Long me, Long target) {
        return blockMapper.countBlock(target, me) > 0;
    }

    /**
     * Ensure user exists in the system.
     *
     * @param userId the user ID to check
     * @throws BizException if user not found
     */
    private void ensureUserExists(Long userId) {
        Optional.ofNullable(userMapper.selectById(userId))
                .orElseThrow(() -> new BizException(CommonStatusEnum.USER_NOT_EXISTS));
    }

    // === Follow operations ===

    @Override
    @Transactional
    public void follow(Long me, Long target) {
        ensureUserExists(target);

        // Use domain model to validate and create
        UserFollow follow = UserFollow.create(me, target);

        // Check block status using domain validation
        UserBlock.ensureNotBlocked(me, target, entry -> isBlocked(entry.getKey(), entry.getValue()));
        UserBlock.ensureNotBlockedBy(me, target, entry -> isBlockedBy(entry.getKey(), entry.getValue()));

        // Ensure not already following
        UserFollow.ensureNotExists(followMapper.countByFollowerAndFollowee(me, target));

        // Persist and update stats
        followMapper.insert(follow);
        UserSocialStats.ensureUpdated(socialStatsMapper.incFollowCount(me, +1));
        UserSocialStats.ensureUpdated(socialStatsMapper.incFansCount(target, +1));
    }

    @Override
    @Transactional
    public void unfollow(Long me, Long target) {
        int deletedCount = followMapper.deleteByFollowerAndFollowee(me, target);
        UserFollow.ensureExists(deletedCount);

        UserSocialStats.ensureUpdated(socialStatsMapper.incFollowCount(me, -1));
        UserSocialStats.ensureUpdated(socialStatsMapper.incFansCount(target, -1));
    }

    // === Block operations ===

    @Override
    @Transactional
    public void block(Long me, Long target) {
        ensureUserExists(target);

        // Use domain model to create block (validates no self-block)
        UserBlock block = UserBlock.create(me, target);

        // Idempotent: only insert if not already blocked
        Optional.of(blockMapper.countBlock(me, target))
                .filter(count -> count == 0)
                .ifPresent(count -> {
                    blockMapper.insert(block);

                    // Remove mutual follow relationships
                    int meFollowsTarget = followMapper.deleteByFollowerAndFollowee(me, target);
                    int targetFollowsMe = followMapper.deleteByFollowerAndFollowee(target, me);

                    // Update stats for deleted follows
                    Optional.of(meFollowsTarget)
                            .filter(c -> c > 0)
                            .ifPresent(c -> {
                                UserSocialStats.ensureUpdated(socialStatsMapper.incFollowCount(me, -1));
                                UserSocialStats.ensureUpdated(socialStatsMapper.incFansCount(target, -1));
                            });

                    Optional.of(targetFollowsMe)
                            .filter(c -> c > 0)
                            .ifPresent(c -> {
                                UserSocialStats.ensureUpdated(socialStatsMapper.incFollowCount(target, -1));
                                UserSocialStats.ensureUpdated(socialStatsMapper.incFansCount(me, -1));
                            });
                });
    }

    @Override
    @Transactional
    public void unblock(Long me, Long target) {
        int deletedCount = blockMapper.deleteByBlockerAndBlocked(me, target);
        UserBlock.ensureExists(deletedCount);
    }

    // === List operations ===

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserBasicInfoResp> followers(Long owner, Long viewer, int limit, String cursor) {
        Cursor parsedCursor = Cursor.parse(cursor);

        // Query follower IDs with limit+1
        List<Long> followerIds = followMapper.findFollowersWithBlockFilter(owner, viewer, limit + 1, parsedCursor.formatTimestamp(), parsedCursor.getId());

        // Batch query user basic info
        List<UserBasicInfoResp> users = userProfileService.getBasicInfoBatch(followerIds);

        return PageResponse.paginate(users, limit, user -> Cursor.build(String.valueOf(user.getUserId()), user.getUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserBasicInfoResp> followings(Long owner, Long viewer, int limit, String cursor) {
        Cursor parsedCursor = Cursor.parse(cursor);

        // Query followee IDs with limit+1
        List<Long> followeeIds = followMapper.findFollowingsWithBlockFilter(owner, viewer, limit + 1, parsedCursor.formatTimestamp(), parsedCursor.getId());

        // Batch query user basic info
        List<UserBasicInfoResp> users = userProfileService.getBasicInfoBatch(followeeIds);

        return PageResponse.paginate(users, limit, user -> Cursor.build(String.valueOf(user.getUserId()), user.getUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserBasicInfoResp> blocks(Long me, int limit, String cursor) {
        Cursor parsedCursor = Cursor.parse(cursor);

        // Query blocked user IDs with limit+1
        List<Long> blockedIds = blockMapper.findBlockedUsers(me, limit + 1, parsedCursor.formatTimestamp(), parsedCursor.getId());

        // Batch query user basic info
        List<UserBasicInfoResp> users = userProfileService.getBasicInfoBatch(blockedIds);

        return PageResponse.paginate(users, limit, user -> Cursor.build(String.valueOf(user.getUserId()), user.getUserId()));
    }
}

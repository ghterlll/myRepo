package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * UserFollow domain model.
 * Rich domain model with business logic and validation.
 */
@Data
@TableName("user_follow")
public class UserFollow {
    @TableId(type = IdType.INPUT) private Long followerId;
    private Long followeeId;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a follow relationship.
     *
     * @param followerId the follower user ID
     * @param followeeId the followee user ID
     * @return new UserFollow instance
     */
    public static UserFollow create(Long followerId, Long followeeId) {
        ensureNotSelfFollow(followerId, followeeId);
        UserFollow follow = new UserFollow();
        follow.setFollowerId(followerId);
        follow.setFolloweeId(followeeId);
        follow.setCreatedAt(LocalDateTime.now());
        return follow;
    }

    /**
     * Ensure user is not trying to follow themselves.
     *
     * @param followerId the follower user ID
     * @param followeeId the followee user ID
     * @throws BizException if attempting to follow self
     */
    private static void ensureNotSelfFollow(Long followerId, Long followeeId) {
        Objects.requireNonNull(followerId, "Follower ID cannot be null");
        Objects.requireNonNull(followeeId, "Followee ID cannot be null");

        Optional.of(followerId)
                .filter(id -> Objects.equals(id, followeeId))
                .ifPresent(id -> {
                    throw new BizException(CommonStatusEnum.CANNOT_FOLLOW_SELF);
                });
    }

    /**
     * Ensure the follow relationship does not exist.
     *
     * @param count the count of existing follow relationships
     * @throws BizException if already following
     */
    public static void ensureNotExists(long count) {
        Optional.of(count)
                .filter(c -> c > 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.ALREADY_FOLLOWING);
                });
    }

    /**
     * Ensure the follow relationship exists.
     *
     * @param count the count of existing follow relationships
     * @throws BizException if not following
     */
    public static void ensureExists(long count) {
        Optional.of(count)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.NOT_FOLLOWING);
                });
    }
}
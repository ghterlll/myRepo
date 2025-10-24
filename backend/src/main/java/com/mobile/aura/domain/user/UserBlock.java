package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * UserBlock domain model.
 * Rich domain model with business logic and validation.
 */
@Data
@TableName("user_block")
public class UserBlock {
    @TableId(type = IdType.INPUT) private Long blockerId;
    private Long blockedId;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a block relationship.
     *
     * @param blockerId the blocker user ID
     * @param blockedId the blocked user ID
     * @return new UserBlock instance
     */
    public static UserBlock create(Long blockerId, Long blockedId) {
        ensureNotSelfBlock(blockerId, blockedId);
        UserBlock block = new UserBlock();
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);
        block.setCreatedAt(LocalDateTime.now());
        return block;
    }

    /**
     * Ensure user is not trying to block themselves.
     *
     * @param blockerId the blocker user ID
     * @param blockedId the blocked user ID
     * @throws BizException if attempting to block self
     */
    private static void ensureNotSelfBlock(Long blockerId, Long blockedId) {
        Objects.requireNonNull(blockerId, "Blocker ID cannot be null");
        Objects.requireNonNull(blockedId, "Blocked ID cannot be null");

        Optional.of(blockerId)
                .filter(id -> Objects.equals(id, blockedId))
                .ifPresent(id -> {
                    throw new BizException(CommonStatusEnum.CANNOT_BLOCK_SELF);
                });
    }

    /**
     * Check if user is blocked by another user.
     *
     * @param blockerId potential blocker user ID
     * @param blockedId potential blocked user ID
     * @param isBlockedChecker mapper to check block status
     * @throws BizException if user is blocked
     */
    public static void ensureNotBlockedBy(Long blockerId, Long blockedId, Function<Map.Entry<Long, Long>, Boolean> isBlockedChecker) {
        Optional.of(Map.entry(blockedId, blockerId))
                .filter(isBlockedChecker::apply)
                .ifPresent(entry -> {
                    throw new BizException(CommonStatusEnum.YOU_ARE_BLOCKED);
                });
    }

    /**
     * Check if user has blocked another user.
     *
     * @param blockerId potential blocker user ID
     * @param blockedId potential blocked user ID
     * @param isBlockedChecker function to check block status
     * @throws BizException if target is blocked
     */
    public static void ensureNotBlocked(Long blockerId, Long blockedId, Function<Map.Entry<Long, Long>, Boolean> isBlockedChecker) {
        Optional.of(Map.entry(blockerId, blockedId))
                .filter(isBlockedChecker::apply)
                .ifPresent(entry -> {
                    throw new BizException(CommonStatusEnum.USER_BLOCKED);
                });
    }

    /**
     * Ensure the block relationship exists.
     *
     * @param count the count of deleted block relationships
     * @throws BizException if not blocking
     */
    public static void ensureExists(long count) {
        Optional.of(count)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.NOT_BLOCKING);
                });
    }
}
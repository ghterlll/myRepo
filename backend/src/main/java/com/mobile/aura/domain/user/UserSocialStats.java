package com.mobile.aura.domain.user;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * User social statistics domain model.
 * Contains cached counts for social interactions.
 */
@Data
@TableName("user_social_stats")
public class UserSocialStats {

    @TableId
    private Long id;

    private Long userId;
    private Integer followCount;
    private Integer fansCount;
    private Integer postCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create initial social stats for new user registration.
     * All counts are initialized to 0.
     *
     * @param userId user ID
     * @return new social stats instance
     */
    public static UserSocialStats createForUser(Long userId) {
        UserSocialStats stats = new UserSocialStats();
        stats.setUserId(userId);
        stats.setFollowCount(0);
        stats.setFansCount(0);
        stats.setPostCount(0);
        LocalDateTime now = LocalDateTime.now();
        stats.setCreatedAt(now);
        stats.setUpdatedAt(now);
        return stats;
    }

    /**
     * Ensure statistics update succeeded.
     *
     * @param updatedCount the number of rows updated
     * @throws BizException if update failed (user not found)
     */
    public static void ensureUpdated(int updatedCount) {
        Optional.of(updatedCount)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.USER_NOT_EXISTS);
                });
    }
}

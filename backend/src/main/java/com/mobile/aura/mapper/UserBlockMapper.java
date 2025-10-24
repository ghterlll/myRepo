package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.UserBlock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserBlockMapper extends BaseMapper<UserBlock> {
    /**
     * Check if either A blocks B or B blocks A.
     * @return count > 0 if blocking exists in either direction
     */
    int existsEither(@Param("a") Long a, @Param("b") Long b);

    /**
     * Count blocks where blocker_id = blockerId and blocked_id = blockedId.
     * @return count of matching blocks
     */
    long countBlock(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);

    /**
     * Get paginated blocked users list.
     * Returns user IDs that the blocker has blocked.
     *
     * @param blockerId the user who blocked others
     * @param limit maximum results (1-100)
     * @param cursorCreatedAt cursor created_at value
     * @param cursorId cursor blocked_id value
     * @return list of blocked user IDs
     */
    List<Long> findBlockedUsers(
            @Param("blockerId") Long blockerId,
            @Param("limit") int limit,
            @Param("cursorCreatedAt") String cursorCreatedAt,
            @Param("cursorId") Long cursorId
    );

    /**
     * Delete block relationship by blocker and blocked.
     *
     * @param blockerId the blocker user ID
     * @param blockedId the blocked user ID
     * @return number of deleted rows
     */
    int deleteByBlockerAndBlocked(@Param("blockerId") Long blockerId, @Param("blockedId") Long blockedId);
}
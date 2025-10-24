package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.user.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /**
     * List all users that the follower follows.
     * @param followerId the follower user ID
     * @return list of follow relationships
     */
    List<UserFollow> listByFollowerId(@Param("followerId") Long followerId);

    /**
     * Get paginated followers list with block filtering.
     * Returns user IDs who follow the owner, excluding blocked users.
     *
     * @param owner the user whose followers to retrieve
     * @param viewer the user viewing the list (null for guest)
     * @param limit maximum results (1-100)
     * @param cursorCreatedAt cursor created_at value
     * @param cursorId cursor follower_id value
     * @return list of follower user IDs
     */
    List<Long> findFollowersWithBlockFilter(
            @Param("owner") Long owner,
            @Param("viewer") Long viewer,
            @Param("limit") int limit,
            @Param("cursorCreatedAt") String cursorCreatedAt,
            @Param("cursorId") Long cursorId
    );

    /**
     * Get paginated followings list with block filtering.
     * Returns user IDs that the owner follows, excluding blocked users.
     *
     * @param owner the user whose followings to retrieve
     * @param viewer the user viewing the list (null for guest)
     * @param limit maximum results (1-100)
     * @param cursorCreatedAt cursor created_at value
     * @param cursorId cursor followee_id value
     * @return list of followee user IDs
     */
    List<Long> findFollowingsWithBlockFilter(
            @Param("owner") Long owner,
            @Param("viewer") Long viewer,
            @Param("limit") int limit,
            @Param("cursorCreatedAt") String cursorCreatedAt,
            @Param("cursorId") Long cursorId
    );

    /**
     * Count follow relationships by follower and followee.
     *
     * @param followerId the follower user ID
     * @param followeeId the followee user ID
     * @return count of matching relationships
     */
    long countByFollowerAndFollowee(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);

    /**
     * Delete follow relationship by follower and followee.
     *
     * @param followerId the follower user ID
     * @param followeeId the followee user ID
     * @return number of deleted rows
     */
    int deleteByFollowerAndFollowee(@Param("followerId") Long followerId, @Param("followeeId") Long followeeId);
}
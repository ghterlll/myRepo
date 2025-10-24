package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * List public posts with cursor-based pagination.
     * @param cursorTimestamp cursor timestamp (nullable)
     * @param cursorId cursor post ID (nullable)
     * @param limit maximum number of results
     * @return list of posts
     */
    List<Post> listPublic(
            @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    /**
     * List posts from followed users with cursor-based pagination.
     * @param followeeIds list of followee user IDs
     * @param cursorTimestamp cursor timestamp (nullable)
     * @param cursorId cursor post ID (nullable)
     * @param limit maximum number of results
     * @return list of posts
     */
    List<Post> listFollowFeed(
            @Param("followeeIds") List<Long> followeeIds,
            @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );
}

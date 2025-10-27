package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.PostBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostBookmarkMapper extends BaseMapper<PostBookmark> {

    /**
     * Count bookmarks for a specific user and post.
     * @param userId the user ID
     * @param postId the post ID
     * @return count of bookmarks
     */
    long countBookmark(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * Delete bookmark by user and post.
     * @param userId the user ID
     * @param postId the post ID
     * @return number of deleted rows
     */
    int deleteBookmark(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * List bookmarked posts by user with pagination (cursor-based).
     * @param userId the user ID
     * @param cursorTimestamp cursor timestamp for pagination
     * @param cursorId cursor ID for pagination
     * @param limit page size (fetch limit+1 for hasMore detection)
     * @return list of bookmarks ordered by created_at DESC
     */
    List<PostBookmark> listByUser(@Param("userId") Long userId,
                                   @Param("cursorTimestamp") String cursorTimestamp,
                                   @Param("cursorId") Long cursorId,
                                   @Param("limit") int limit);
}
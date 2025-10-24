package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.PostBookmark;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    /**
     * Count likes for a specific user and post.
     * @param userId the user ID
     * @param postId the post ID
     * @return count of likes
     */
    long countLike(@Param("userId") Long userId, @Param("postId") Long postId);

    /**
     * Delete like by user and post.
     * @param userId the user ID
     * @param postId the post ID
     * @return number of deleted rows
     */
    int deleteLike(@Param("userId") Long userId, @Param("postId") Long postId);
}
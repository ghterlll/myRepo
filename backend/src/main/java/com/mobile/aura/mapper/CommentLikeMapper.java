package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    /**
     * Count likes for a specific user and comment.
     * @param userId the user ID
     * @param commentId the comment ID
     * @return count of likes
     */
    long countLike(@Param("userId") Long userId, @Param("commentId") Long commentId);

    /**
     * Delete like by user and comment.
     * @param userId the user ID
     * @param commentId the comment ID
     * @return number of deleted rows
     */
    int deleteLike(@Param("userId") Long userId, @Param("commentId") Long commentId);
}

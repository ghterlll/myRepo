package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.PostMedia;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostMediaMapper extends BaseMapper<PostMedia> {

    /**
     * List all media for a post, ordered by sort_order.
     * @param postId the post ID
     * @return list of media items
     */
    List<PostMedia> listByPostId(@Param("postId") Long postId);

    /**
     * Find the first media item for a post (cover image).
     * @param postId the post ID
     * @return first media item or null
     */
    PostMedia findFirstByPostId(@Param("postId") Long postId);

    /**
     * Delete all media for a post.
     * Batch delete operation - may delete 0 rows if post has no media.
     * @param postId the post ID
     */
    void deleteByPostId(@Param("postId") Long postId);
}

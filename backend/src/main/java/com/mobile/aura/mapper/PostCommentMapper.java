package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.PostComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PostCommentMapper extends BaseMapper<PostComment> {

    /**
     * Increment reply count for a comment.
     * @param id the comment ID
     * @param delta the increment value (can be negative)
     * @return number of updated rows
     */
    int incReplyCount(@Param("id") Long id, @Param("delta") int delta);

    /**
     * List root comments for a post with cursor-based pagination.
     * @param postId the post ID
     * @param cursorTimestamp cursor timestamp (nullable)
     * @param cursorId cursor comment ID (nullable)
     * @param limit maximum number of results
     * @return list of root comments
     */
    List<PostComment> listRootComments(
            @Param("postId") Long postId,
            @Param("cursorTimestamp") String cursorTimestamp,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );

    /**
     * List preview replies for a root comment.
     * @param postId the post ID
     * @param rootId the root comment ID
     * @param previewSize maximum number of preview replies
     * @return list of preview replies
     */
    List<PostComment> listPreviewReplies(
            @Param("postId") Long postId,
            @Param("rootId") Long rootId,
            @Param("previewSize") int previewSize
    );

    /**
     * List all replies for a root comment with cursor-based pagination.
     * @param postId the post ID
     * @param rootId the root comment ID
     * @param cursorTimestamp cursor timestamp (nullable)
     * @param cursorId cursor comment ID (nullable)
     * @param limit maximum number of results
     * @return list of replies
     */
    List<PostComment> listReplies(
            @Param("postId") Long postId,
            @Param("rootId") Long rootId,
            @Param("cursorTimestamp") String cursorTimestamp,
            @Param("cursorId") Long cursorId,
            @Param("limit") int limit
    );
}

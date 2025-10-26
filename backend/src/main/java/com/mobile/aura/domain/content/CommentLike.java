package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Comment like domain model.
 * Represents a user's like on a comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_comment_like")
public class CommentLike {

    private Long commentId;
    private Long userId;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a new comment like.
     *
     * @param userId the ID of the user
     * @param commentId the ID of the comment
     * @return new CommentLike instance
     */
    public static CommentLike create(Long userId, Long commentId) {
        return CommentLike.builder()
                .commentId(commentId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Ensure a comment like doesn't already exist.
     *
     * @param count the count of existing likes
     * @throws BizException if like already exists
     */
    public static void ensureNotExists(long count) {
        if (count > 0) {
            throw new BizException(CommonStatusEnum.ALREADY_LIKED);
        }
    }

    /**
     * Ensure a comment like was successfully deleted.
     *
     * @param deletedCount the number of deleted rows
     * @throws BizException if no like was deleted
     */
    public static void ensureDeleted(int deletedCount) {
        if (deletedCount == 0) {
            throw new BizException(CommonStatusEnum.NOT_LIKED);
        }
    }
}

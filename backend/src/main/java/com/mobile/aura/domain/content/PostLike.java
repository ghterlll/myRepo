package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * PostLike domain model.
 * Represents a like on a post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_like")
public class PostLike {
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a new like.
     *
     * @param userId the user ID
     * @param postId the post ID
     * @return new PostLike instance
     */
    public static PostLike create(Long userId, Long postId) {
        return new PostLike(postId, userId, LocalDateTime.now());
    }

    /**
     * Ensure like does not already exist.
     *
     * @param count the count from database
     * @throws BizException if already liked
     */
    public static void ensureNotExists(long count) {
        Optional.of(count)
                .filter(c -> c == 0)
                .orElseThrow(() -> new BizException(CommonStatusEnum.ALREADY_LIKED));
    }

    /**
     * Ensure unlike operation is valid (like must exist).
     *
     * @param deletedCount the number of rows deleted
     * @throws BizException if not previously liked
     */
    public static void ensureDeleted(int deletedCount) {
        Optional.of(deletedCount)
                .filter(c -> c > 0)
                .orElseThrow(() -> new BizException(CommonStatusEnum.NOT_LIKED));
    }
}

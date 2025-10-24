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
 * PostBookmark domain model.
 * Represents a bookmark on a post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_bookmark")
public class PostBookmark {
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a new bookmark.
     *
     * @param userId the user ID
     * @param postId the post ID
     * @return new PostBookmark instance
     */
    public static PostBookmark create(Long userId, Long postId) {
        return new PostBookmark(postId, userId, LocalDateTime.now());
    }

    /**
     * Ensure bookmark does not already exist.
     *
     * @param count the count from database
     * @throws BizException if already bookmarked
     */
    public static void ensureNotExists(long count) {
        Optional.of(count)
                .filter(c -> c == 0)
                .orElseThrow(() -> new BizException(CommonStatusEnum.ALREADY_BOOKMARKED));
    }

    /**
     * Ensure unbookmark operation is valid (bookmark must exist).
     *
     * @param deletedCount the number of rows deleted
     * @throws BizException if not previously bookmarked
     */
    public static void ensureDeleted(int deletedCount) {
        Optional.of(deletedCount)
                .filter(c -> c > 0)
                .orElseThrow(() -> new BizException(CommonStatusEnum.NOT_BOOKMARKED));
    }
}

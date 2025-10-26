package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.Cursor;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.post.CommentResp;
import com.mobile.aura.dto.post.CommentThreadResp;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Post comment domain model.
 * Represents a comment on a post with support for nested replies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_comment")
public class PostComment {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long authorId;
    private Long rootId;     // Root comment ID (for nested comments)
    private Long parentId;   // Parent comment ID (for direct replies)
    private String content;
    private Integer replyCount; // Number of direct replies
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    /**
     * Factory method to create a root-level comment.
     *
     * @param postId the post ID
     * @param authorId the author ID
     * @param content the comment content
     * @return new PostComment instance (rootId will be set after insertion)
     */
    public static PostComment createRoot(Long postId, Long authorId, String content) {
        validateContent(content);
        return new PostComment(
                null,
                postId,
                authorId,
                null, // Will be set to self ID after insertion
                null,
                content.trim(),
                0,
                LocalDateTime.now(),
                null
        );
    }

    /**
     * Factory method to create a reply comment.
     *
     * @param postId the post ID
     * @param authorId the author ID
     * @param parent the parent comment
     * @param content the comment content
     * @param blockChecker function to check if users have blocked each other
     * @return new PostComment instance
     */
    public static PostComment createReply(Long postId, Long authorId, PostComment parent, String content, BiPredicate<Long, Long> blockChecker) {
        validateContent(content);
        parent.ensureExists();
        parent.ensureAccessible(authorId, blockChecker);

        Long rootId = Optional.ofNullable(parent.getRootId()).orElse(parent.getId());

        return new PostComment(
                null,
                postId,
                authorId,
                rootId,
                parent.getId(),
                content.trim(),
                0,
                LocalDateTime.now(),
                null
        );
    }

    /**
     * Make this comment its own root (for root-level comments).
     */
    public void becomeRoot() {
        this.rootId = this.id;
    }

    /**
     * Convert this entity to CommentResp DTO.
     *
     * @return CommentResp DTO
     */
    public CommentResp toCommentResp() {
        return new CommentResp(
                this.id,
                this.postId,
                this.authorId,
                this.rootId,
                this.parentId,
                this.content,
                this.createdAt == null ? null : FORMATTER.format(this.createdAt),
                this.replyCount
        );
    }

    /**
     * Mark this comment as deleted.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Check if this comment is deleted.
     *
     * @return true if deleted, false otherwise
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Ensure comment exists (not null and not deleted).
     *
     * @throws BizException if comment doesn't exist or is deleted
     */
    public void ensureExists() {
        Optional.of(this)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BizException(CommonStatusEnum.COMMENT_NOT_FOUND));
    }

    /**
     * Ensure comment is accessible (no blocks between users).
     *
     * @param userId the user ID checking access
     * @param blockChecker function to check if users have blocked each other
     * @throws BizException if access is blocked
     */
    public void ensureAccessible(Long userId, BiPredicate<Long, Long> blockChecker) {
        Optional.of(blockChecker.test(userId, this.authorId))
                .filter(blocked -> !blocked)
                .orElseThrow(() -> new BizException(CommonStatusEnum.POST_NOT_FOUND));
    }

    /**
     * Check if user can delete this comment.
     *
     * @param userId the user ID
     * @param postAuthorId the post author ID
     * @return true if user can delete
     */
    public boolean canBeDeletedBy(Long userId, Long postAuthorId) {
        return Objects.equals(userId, this.authorId) || Objects.equals(userId, postAuthorId);
    }

    /**
     * Validate comment content.
     *
     * @param content the content to validate
     * @throws BizException if content is invalid
     */
    private static void validateContent(String content) {
        Optional.ofNullable(content)
                .filter(c -> !c.isBlank())
                .orElseThrow(() -> new BizException(CommonStatusEnum.INVALID_PARAM));
    }

    /**
     * Ensure comment reply count update succeeded.
     *
     * @param updatedCount the number of rows updated
     * @throws BizException if update failed (comment not found)
     */
    public static void ensureUpdated(int updatedCount) {
        Optional.of(updatedCount)
                .filter(c -> c == 0)
                .ifPresent(c -> {
                    throw new BizException(CommonStatusEnum.COMMENT_NOT_FOUND);
                });
    }

    /**
     * Build a paginated thread response from root comments with preview replies.
     * Encapsulates pagination logic for comment threads.
     *
     * @param rootComments query results (limit + 1 items)
     * @param limit the page size limit
     * @param repliesProvider function to get preview replies for each root comment ID
     * @return PageResponse with comment threads, cursor, and pagination metadata
     */
    public static PageResponse<CommentThreadResp> toThreadsPageResponse(
            List<PostComment> rootComments,
            int limit,
            Function<Long, List<CommentResp>> repliesProvider) {
        List<CommentThreadResp> threads = rootComments.stream()
                .map(root -> new CommentThreadResp(
                        root.toCommentResp(),
                        repliesProvider.apply(root.getId())))
                .toList();

        return PageResponse.paginate(
                threads,
                limit,
                thread -> Cursor.build(thread.getRoot().getCreatedAt(), thread.getRoot().getId())
        );
    }

    /**
     * Build a paginated reply response from comment replies.
     * Encapsulates pagination logic for comment replies.
     *
     * @param replies query results (limit + 1 items)
     * @param limit the page size limit
     * @return PageResponse with comment replies, cursor, and pagination metadata
     */
    public static PageResponse<CommentResp> toRepliesPageResponse(
            List<PostComment> replies,
            int limit) {
        List<CommentResp> commentResps = replies.stream()
                .map(PostComment::toCommentResp)
                .toList();

        return PageResponse.paginate(
                commentResps,
                limit,
                reply -> Cursor.build(reply.getCreatedAt(), reply.getId())
        );
    }
}
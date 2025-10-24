package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.*;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.constant.PostVisibility;
import com.mobile.aura.dto.Cursor;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.post.MediaItem;
import com.mobile.aura.dto.post.PostCardResp;
import com.mobile.aura.dto.post.PostCreateReq;
import com.mobile.aura.dto.post.PostDetailResp;
import com.mobile.aura.support.BizException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Post domain model (Aggregate Root).
 * Represents a social media post with rich business logic for status transitions,
 * validation, and access control.
 *
 * <p>This is a Rich Domain Model that encapsulates business rules and ensures
 * invariants are maintained. All state changes should go through domain methods
 * rather than direct setters.
 *
 * @author Aura Team
 * @version 2.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("posts")
public class Post {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @TableId
    private Long id;
    private String title;
    private Long authorId;
    private String caption;
    private String visibility;
    private Integer mediaCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String status;

    /**
     * Factory method to create a new post from a request.
     *
     * @param authorId the ID of the post author
     * @param req the creation request
     * @return new Post instance
     */
    public static Post create(Long authorId, PostCreateReq req) {
        validateTitle(req.getTitle());

        return Post.builder()
                .authorId(authorId)
                .title(req.getTitle().trim())
                .caption(req.getCaption())
                .visibility(Boolean.TRUE.equals(req.getPublish()) ? PostVisibility.PUBLIC : PostVisibility.DRAFT)
                .status("0")
                .mediaCount(req.getMedias() == null ? 0 : req.getMedias().size())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Update post title.
     *
     * @param newTitle new title to set
     */
    public void updateTitle(String newTitle) {
        if (newTitle != null) {
            String trimmed = newTitle.trim();
            if (trimmed.isEmpty()) {
                throw new BizException(CommonStatusEnum.TITLE_REQUIRED);
            }
            this.title = trimmed;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Update post caption.
     *
     * @param newCaption new caption to set
     */
    public void updateCaption(String newCaption) {
        if (newCaption != null) {
            this.caption = newCaption;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * Publish the post (change visibility to PUBLIC).
     */
    public void publish() {
        this.visibility = PostVisibility.PUBLIC;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Hide the post (change visibility to DRAFT).
     */
    public void hide() {
        this.visibility = PostVisibility.DRAFT;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Soft delete the post.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Update media count.
     *
     * @param count new media count
     */
    public void updateMediaCount(int count) {
        this.mediaCount = count;
    }

    /**
     * Check if the post is deleted.
     *
     * @return true if deleted, false otherwise
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * Ensure the post exists (not null and not deleted).
     *
     * @param post the post to check
     * @throws BizException if post is null or deleted
     */
    public static void ensureExists(Post post) {
        if (post == null || post.isDeleted()) {
            throw new BizException(CommonStatusEnum.POST_NOT_FOUND);
        }
    }

    /**
     * Check if a user can modify this post (must be the author).
     *
     * @param userId the user ID to check
     * @throws BizException if user is not authorized
     */
    public void ensureCanModify(Long userId) {
        if (!Objects.equals(this.authorId, userId)) {
            throw new BizException(CommonStatusEnum.FORBIDDEN);
        }
    }

    /**
     * Ensure the post is modifiable by the given user.
     * Combines existence check and authorization check.
     *
     * @param userId the user ID to check
     * @throws BizException if post is not found or user is not authorized
     */
    public void ensureModifiable(Long userId) {
        ensureExists(this);
        ensureCanModify(userId);
    }

    /**
     * Check if the post is readable (not deleted and has appropriate visibility).
     *
     * @throws BizException if post is not readable
     */
    public void ensureReadable() {
        ensureExists(this);
        if (!Objects.equals(this.visibility, PostVisibility.PUBLIC)) {
            throw new BizException(CommonStatusEnum.FORBIDDEN);
        }
    }

    /**
     * Check if the post is readable by a specific viewer.
     * Includes existence, visibility, and custom block check.
     *
     * @param viewer the viewer ID (can be null for anonymous users)
     * @param blockChecker function to check if viewer and author have blocked each other
     * @throws BizException if post is not readable
     */
    public void ensureReadableBy(Long viewer, java.util.function.BiPredicate<Long, Long> blockChecker) {
        ensureReadable();
        if (viewer != null && blockChecker != null && blockChecker.test(viewer, this.authorId)) {
            throw new BizException(CommonStatusEnum.POST_NOT_FOUND); // Return 404 for blocked users
        }
    }

    /**
     * Convert this entity to PostDetailResp DTO.
     *
     * @param medias list of media items
     * @param tags list of tags
     * @return PostDetailResp DTO
     */
    public PostDetailResp toDetailResp(List<MediaItem> medias, List<String> tags) {
        return new PostDetailResp(
                this.id,
                this.authorId,
                this.title,
                this.caption,
                this.visibility,
                tags,
                medias,
                this.createdAt == null ? null : FORMATTER.format(this.createdAt),
                this.updatedAt == null ? null : FORMATTER.format(this.updatedAt)
        );
    }

    /**
     * Convert this entity to PostCardResp DTO.
     *
     * @param coverUrl URL of the cover image
     * @return PostCardResp DTO
     */
    public PostCardResp toCardResp(String coverUrl) {
        return new PostCardResp(
                this.id,
                coverUrl,
                this.authorId,
                this.title,
                this.createdAt == null ? null : FORMATTER.format(this.createdAt)
        );
    }

    /**
     * Convert PostMedia to MediaItem DTO.
     *
     * @param media PostMedia entity
     * @return MediaItem DTO
     */
    public static MediaItem toMediaItem(PostMedia media) {
        return new MediaItem(
                media.getUrl(),
                media.getWidth(),
                media.getHeight(),
                media.getSortOrder(),
                media.getObjectKey(),      // S3 key for file management
                media.getMimeType(),       // Content type
                media.getBytes(),          // File size
                media.getChecksum(),       // File checksum
                media.getBlurhash()        // Progressive loading hash
        );
    }

    /**
     * Validate title is not blank.
     *
     * @param title title to validate
     * @throws BizException if title is invalid
     */
    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new BizException(CommonStatusEnum.TITLE_REQUIRED);
        }
    }

    /**
     * Build a paginated card response from query results.
     * Encapsulates pagination logic for post cards.
     *
     * @param posts query results (limit + 1 items)
     * @param limit the page size limit
     * @param coverUrlProvider function to get cover URL for each post ID
     * @return PageResponse with post cards, cursor, and pagination metadata
     */
    public static PageResponse<PostCardResp> toCardsPageResponse(
            List<Post> posts,
            int limit,
            Function<Long, String> coverUrlProvider) {
        List<PostCardResp> cards = posts.stream()
                .map(post -> post.toCardResp(coverUrlProvider.apply(post.getId())))
                .toList();

        return PageResponse.paginate(
                cards,
                limit,
                card -> Cursor.build(card.getCreatedAt(), card.getId())
        );
    }
}

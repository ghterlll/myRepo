package com.mobile.aura.service;

import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.post.*;

import java.util.List;

/**
 * Service interface for post-related operations.
 * Handles post CRUD, interactions (like, bookmark), and comments.
 */
public interface PostService {

    // ========= Post CRUD & List Operations =========

    /**
     * Create a new post.
     *
     * @param authorId the ID of the post author
     * @param req the post creation request containing title, caption, and media
     * @return the ID of the newly created post
     * @throws com.mobile.aura.support.BizException if title is blank or validation fails
     */
    Long create(Long authorId, PostCreateReq req);

    /**
     * Update an existing post.
     *
     * @param postId the ID of the post to update
     * @param authorId the ID of the user attempting the update (must be the author)
     * @param req the update request containing new title and/or caption
     * @throws com.mobile.aura.support.BizException if post not found, user not authorized, or validation fails
     */
    void update(Long postId, Long authorId, PostUpdateReq req);

    /**
     * Replace all media items in a post.
     *
     * @param postId the ID of the post
     * @param authorId the ID of the user attempting the operation (must be the author)
     * @param medias the new list of media items to replace existing ones
     * @throws com.mobile.aura.support.BizException if post not found or user not authorized
     */
    void replaceMedia(Long postId, Long authorId, List<MediaItem> medias);

    /**
     * Publish a post (change visibility to PUBLIC).
     *
     * @param postId the ID of the post to publish
     * @param authorId the ID of the user attempting the operation (must be the author)
     * @throws com.mobile.aura.support.BizException if post not found or user not authorized
     */
    void publish(Long postId, Long authorId);

    /**
     * Hide a post (change visibility to DRAFT).
     *
     * @param postId the ID of the post to hide
     * @param authorId the ID of the user attempting the operation (must be the author)
     * @throws com.mobile.aura.support.BizException if post not found or user not authorized
     */
    void hide(Long postId, Long authorId);

    /**
     * Soft delete a post.
     *
     * @param postId the ID of the post to delete
     * @param authorId the ID of the user attempting the operation (must be the author)
     * @throws com.mobile.aura.support.BizException if user not authorized
     */
    void delete(Long postId, Long authorId);

    /**
     * Get detailed information about a post.
     *
     * @param viewer the ID of the user viewing the post (for permission checks)
     * @param postId the ID of the post to retrieve
     * @return detailed post information including media and tags
     * @throws com.mobile.aura.support.BizException if post not found, deleted, or not accessible
     */
    PostDetailResp detail(Long viewer, Long postId);

    /**
     * List public posts with pagination.
     *
     * @param viewer the ID of the user viewing the list (for filtering blocked users)
     * @param limit maximum number of posts to return (1-100)
     * @param cursor pagination cursor (format: "timestamp|id")
     * @return paginated response with post cards
     */
    PageResponse<PostCardResp> listPublic(Long viewer, int limit, String cursor);

    /**
     * List posts from users that the viewer follows.
     *
     * @param viewer the ID of the user viewing the feed
     * @param limit maximum number of posts to return (1-100)
     * @param cursor pagination cursor (format: "timestamp|id")
     * @return paginated response with post cards from followed users
     */
    PageResponse<PostCardResp> listFollowFeed(Long viewer, int limit, String cursor);

    // ========= Like Operations =========

    /**
     * Like a post.
     *
     * @param userId the ID of the user liking the post
     * @param postId the ID of the post to like
     * @throws com.mobile.aura.support.BizException if post not found, not accessible, or already liked
     */
    void like(Long userId, Long postId);

    /**
     * Unlike a post (remove like).
     *
     * @param userId the ID of the user unliking the post
     * @param postId the ID of the post to unlike
     * @throws com.mobile.aura.support.BizException if post was not liked
     */
    void unlike(Long userId, Long postId);

    // ========= Bookmark Operations =========

    /**
     * Bookmark a post.
     *
     * @param userId the ID of the user bookmarking the post
     * @param postId the ID of the post to bookmark
     * @throws com.mobile.aura.support.BizException if post not found, not accessible, or already bookmarked
     */
    void bookmark(Long userId, Long postId);

    /**
     * Remove bookmark from a post.
     *
     * @param userId the ID of the user removing the bookmark
     * @param postId the ID of the post to unbookmark
     * @throws com.mobile.aura.support.BizException if post was not bookmarked
     */
    void unbookmark(Long userId, Long postId);

    // ========= Comment Operations (Nested Comments) =========

    /**
     * Create a comment on a post.
     * Supports nested comments (replies to other comments).
     *
     * @param userId the ID of the user creating the comment
     * @param postId the ID of the post to comment on
     * @param req the comment creation request containing content and optional parent comment ID
     * @return the ID of the newly created comment
     * @throws com.mobile.aura.support.BizException if post not found, content is blank, or parent comment invalid
     */
    Long createComment(Long userId, Long postId, CommentCreateReq req);

    /**
     * Delete a comment.
     * Only the comment author or post author can delete the comment.
     *
     * @param userId the ID of the user attempting to delete the comment
     * @param commentId the ID of the comment to delete
     * @throws com.mobile.aura.support.BizException if user not authorized
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * List root-level comments for a post with pagination.
     * Includes a preview of nested replies for each root comment.
     *
     * @param viewer the ID of the user viewing the comments
     * @param postId the ID of the post
     * @param limit maximum number of root comments to return (1-100)
     * @param cursor pagination cursor (format: "timestamp|id")
     * @param previewSize number of nested replies to preview for each root comment
     * @return paginated response with comment threads (root comment + preview replies)
     * @throws com.mobile.aura.support.BizException if post not found or not accessible
     */
    PageResponse<CommentThreadResp> listRootComments(Long viewer, Long postId, int limit, String cursor, int previewSize);

    /**
     * List all replies to a specific root comment with pagination.
     *
     * @param viewer the ID of the user viewing the replies
     * @param rootCommentId the ID of the root comment
     * @param limit maximum number of replies to return (1-100)
     * @param cursor pagination cursor (format: "timestamp|id")
     * @return paginated response with reply comments
     * @throws com.mobile.aura.support.BizException if root comment not found or post not accessible
     */
    PageResponse<CommentResp> listReplies(Long viewer, Long rootCommentId, int limit, String cursor);
}

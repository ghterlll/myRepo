package com.mobile.aura.controller;

import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.post.*;
import com.mobile.aura.service.PostService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for post-related operations.
 * Provides endpoints for post CRUD operations, social interactions (like, bookmark),
 * and comment management with nested reply support.
 *
 * <p>All endpoints require authentication via JWT token.
 *
 * @author Aura Team
 * @version 1.0
 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/post")
public class PostController {

    private static final String ATTR_USER_ID = JwtAuthInterceptor.ATTR_USER_ID;

    private final PostService postService;

    // ==================== Post CRUD Operations ====================

    /**
     * Create a new post.
     *
     * @param userId the authenticated user ID
     * @param req post creation request with title, caption, and media
     * @return response containing the newly created post ID
     */
    @PostMapping
    public ResponseResult<Map<String, Long>> create(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @Valid @RequestBody PostCreateReq req) {
        Long postId = postService.create(userId, req);
        return ResponseResult.success(Map.of("id", postId));
    }

    /**
     * Update an existing post.
     *
     * @param userId the authenticated user ID (must be post author)
     * @param postId the post ID to update
     * @param req update request with new title and/or caption
     * @return success response
     */
    @PatchMapping("/{postId}")
    public ResponseResult<Void> update(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateReq req) {
        postService.update(postId, userId, req);
        return ResponseResult.success();
    }

    /**
     * Replace all media items in a post.
     *
     * @param userId the authenticated user ID (must be post author)
     * @param postId the post ID
     * @param medias new list of media items
     * @return success response
     */
    @PutMapping("/{postId}/media")
    public ResponseResult<Void> replaceMedia(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId,
            @RequestBody List<@Valid MediaItem> medias) {
        postService.replaceMedia(postId, userId, medias);
        return ResponseResult.success();
    }

    /**
     * Publish a post (make it publicly visible).
     *
     * @param userId the authenticated user ID (must be post author)
     * @param postId the post ID to publish
     * @return success response
     */
    @PostMapping("/{postId}/publish")
    public ResponseResult<Void> publish(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.publish(postId, userId);
        return ResponseResult.success();
    }

    /**
     * Hide a post (change to draft status).
     *
     * @param userId the authenticated user ID (must be post author)
     * @param postId the post ID to hide
     * @return success response
     */
    @PostMapping("/{postId}/hide")
    public ResponseResult<Void> hide(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.hide(postId, userId);
        return ResponseResult.success();
    }

    /**
     * Delete a post (soft delete).
     *
     * @param userId the authenticated user ID (must be post author)
     * @param postId the post ID to delete
     * @return success response
     */
    @DeleteMapping("/{postId}")
    public ResponseResult<Void> delete(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.delete(postId, userId);
        return ResponseResult.success();
    }

    /**
     * Get detailed information about a post.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to retrieve
     * @return post details including media and tags
     */
    @GetMapping("/{postId}")
    public ResponseResult<PostDetailResp> detail(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        return ResponseResult.success(postService.detail(userId, postId));
    }

    // ==================== Post List Operations ====================

    /**
     * List public posts with cursor-based pagination.
     *
     * @param userId the authenticated user ID
     * @param req pagination request with limit and cursor
     * @return paginated response with post cards
     */
    @GetMapping
    public ResponseResult<PageResponse<PostCardResp>> listPublic(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @Valid @ModelAttribute PostListReq req) {
        return ResponseResult.success(postService.listPublic(userId, req.getLimit(), req.getCursor()));
    }

    /**
     * List posts from users that the current user follows.
     *
     * @param userId the authenticated user ID
     * @param req pagination request with limit and cursor
     * @return paginated response with post cards from followed users
     */
    @GetMapping("/feed/followings")
    public ResponseResult<PageResponse<PostCardResp>> followFeed(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @Valid @ModelAttribute PostListReq req) {
        return ResponseResult.success(postService.listFollowFeed(userId, req.getLimit(), req.getCursor()));
    }

    /**
     * Search public posts by keyword with optional category filter.
     *
     * @param userId the authenticated user ID
     * @param req search request with keyword, category, limit, and cursor
     * @return paginated response with post cards matching the search criteria
     */
    @GetMapping("/search")
    public ResponseResult<PageResponse<PostCardResp>> search(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @Valid @ModelAttribute PostSearchReq req) {
        return ResponseResult.success(postService.searchPublic(
                userId, req.getKeyword(), req.getCategory(), req.getLimit(), req.getCursor()));
    }

    /**
     * List posts created by the current user (my posts).
     * Includes all posts regardless of status (draft, published, hidden).
     *
     * @param userId the authenticated user ID
     * @param req pagination request with limit and cursor
     * @return paginated response with post cards created by the current user
     */
    @GetMapping("/me")
    public ResponseResult<PageResponse<PostCardResp>> listMyPosts(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @Valid @ModelAttribute PostListReq req) {
        return ResponseResult.success(postService.listMyPosts(userId, req.getLimit(), req.getCursor()));
    }

    /**
     * List posts bookmarked by the current user.
     *
     * @param userId the authenticated user ID
     * @param req pagination request with limit and cursor
     * @return paginated response with bookmarked post cards
     */
    @GetMapping("/bookmarks")
    public ResponseResult<PageResponse<PostCardResp>> listBookmarkedPosts(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @Valid @ModelAttribute PostListReq req) {
        return ResponseResult.success(postService.listBookmarkedPosts(userId, req.getLimit(), req.getCursor()));
    }

    // ==================== Social Interaction Operations ====================

    /**
     * Like a post.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to like
     * @return success response
     */
    @PostMapping("/{postId}/like")
    public ResponseResult<Void> like(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.like(userId, postId);
        return ResponseResult.success();
    }

    /**
     * Check if the current user has liked a post.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to check
     * @return response containing isLiked status
     */
    @GetMapping("/{postId}/like/status")
    public ResponseResult<Map<String, Boolean>> checkLike(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        boolean isLiked = postService.isLiked(userId, postId);
        return ResponseResult.success(Map.of("isLiked", isLiked));
    }

    /**
     * Unlike a post (remove like).
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to unlike
     * @return success response
     */
    @DeleteMapping("/{postId}/like")
    public ResponseResult<Void> unlike(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.unlike(userId, postId);
        return ResponseResult.success();
    }

    /**
     * Bookmark a post.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to bookmark
     * @return success response
     */
    @PostMapping("/{postId}/bookmark")
    public ResponseResult<Void> bookmark(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.bookmark(userId, postId);
        return ResponseResult.success();
    }

    /**
     * Check if the current user has bookmarked a post.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to check
     * @return response containing isBookmarked status
     */
    @GetMapping("/{postId}/bookmark/status")
    public ResponseResult<Map<String, Boolean>> checkBookmark(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        boolean isBookmarked = postService.isBookmarked(userId, postId);
        return ResponseResult.success(Map.of("isBookmarked", isBookmarked));
    }

    /**
     * Remove bookmark from a post.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to unbookmark
     * @return success response
     */
    @DeleteMapping("/{postId}/bookmark")
    public ResponseResult<Void> unbookmark(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId) {
        postService.unbookmark(userId, postId);
        return ResponseResult.success();
    }

    // ==================== Comment Operations ====================

    /**
     * Create a comment on a post.
     * Supports nested comments by providing a parent comment ID.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID to comment on
     * @param req comment creation request with content and optional parent ID
     * @return response containing the newly created comment ID
     */
    @PostMapping("/{postId}/comments")
    public ResponseResult<Map<String, Long>> createComment(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateReq req) {
        Long commentId = postService.createComment(userId, postId, req);
        return ResponseResult.success(Map.of("id", commentId));
    }

    /**
     * Delete a comment.
     * Only the comment author or post author can delete the comment.
     *
     * @param userId the authenticated user ID
     * @param commentId the comment ID to delete
     * @return success response
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseResult<Void> deleteComment(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long commentId) {
        postService.deleteComment(userId, commentId);
        return ResponseResult.success();
    }

    /**
     * List root-level comments for a post with pagination.
     * Includes a preview of nested replies for each root comment.
     *
     * @param userId the authenticated user ID
     * @param postId the post ID
     * @param req pagination request with limit, cursor, and preview size
     * @return paginated response with comment threads (root comment + preview replies)
     */
    @GetMapping("/{postId}/comments")
    public ResponseResult<PageResponse<CommentThreadResp>> listRootComments(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long postId,
            @Valid @ModelAttribute CommentListReq req) {
        return ResponseResult.success(postService.listRootComments(
                userId, postId, req.getLimit(), req.getCursor(), req.getPreviewSize()));
    }

    /**
     * List all replies to a specific root comment with pagination.
     *
     * @param userId the authenticated user ID
     * @param rootCommentId the ID of the root comment
     * @param req pagination request with limit and cursor
     * @return paginated response with reply comments
     */
    @GetMapping("/comments/{rootCommentId}/replies")
    public ResponseResult<PageResponse<CommentResp>> listReplies(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long rootCommentId,
            @Valid @ModelAttribute PostListReq req) {
        return ResponseResult.success(postService.listReplies(userId, rootCommentId, req.getLimit(), req.getCursor()));
    }

    // ==================== Comment Like Operations ====================

    /**
     * Like a comment.
     *
     * @param userId the authenticated user ID
     * @param commentId the comment ID to like
     * @return success response
     */
    @PostMapping("/comments/{commentId}/like")
    public ResponseResult<Void> likeComment(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long commentId) {
        postService.likeComment(userId, commentId);
        return ResponseResult.success();
    }

    /**
     * Unlike a comment (remove like).
     *
     * @param userId the authenticated user ID
     * @param commentId the comment ID to unlike
     * @return success response
     */
    @DeleteMapping("/comments/{commentId}/like")
    public ResponseResult<Void> unlikeComment(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long commentId) {
        postService.unlikeComment(userId, commentId);
        return ResponseResult.success();
    }

    /**
     * Check if the current user has liked a comment.
     *
     * @param userId the authenticated user ID
     * @param commentId the comment ID to check
     * @return response containing isLiked status
     */
    @GetMapping("/comments/{commentId}/like/status")
    public ResponseResult<Map<String, Boolean>> checkCommentLike(
            @RequestAttribute(ATTR_USER_ID) Long userId,
            @PathVariable Long commentId) {
        boolean isLiked = postService.isCommentLiked(userId, commentId);
        return ResponseResult.success(Map.of("isLiked", isLiked));
    }
}

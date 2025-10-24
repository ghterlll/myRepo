package com.mobile.aura.service;

import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.tag.TagDtos.*;
import com.mobile.aura.dto.post.PostCardResp;

import java.util.List;

/**
 * Service interface for tag management and tag-post associations.
 * Provides operations for CRUD on tags, managing post-tag relationships,
 * and querying posts by tags with automatic blocking filter.
 */
public interface TagService {

    /**
     * Creates a new tag with the specified name.
     * If a tag with the same name (case-insensitive) already exists,
     * returns the ID of the existing tag, making this operation idempotent.
     *
     * @param req the tag creation request containing the tag name
     * @return the ID of the created or existing tag
     * @throws com.mobile.aura.support.BizException if the tag name is blank
     */
    Long create(TagCreateReq req);

    /**
     * Updates an existing tag's properties.
     * Currently supports updating the tag name while maintaining
     * case-insensitive uniqueness.
     *
     * @param tagId the ID of the tag to update
     * @param req the tag update request containing new values
     * @throws com.mobile.aura.support.BizException if the tag is not found or name is invalid
     */
    void update(Long tagId, TagUpdateReq req);

    /**
     * Permanently deletes a tag and all its associations with posts.
     * This is a hard delete operation that cascades to post_tags relationships.
     *
     * @param tagId the ID of the tag to delete
     */
    void delete(Long tagId);

    /**
     * Lists tags with optional keyword search and cursor-based pagination.
     * Tags are sorted alphabetically by name (case-insensitive).
     * The cursor is based on the normalized lowercase name (name_lc).
     *
     * @param keyword optional search keyword for case-insensitive name filtering (can be null)
     * @param limit maximum number of tags to return (will be normalized to 1-100)
     * @param cursor optional pagination cursor from previous response (can be null)
     * @return paginated response containing tags, next cursor, and hasMore flag
     */
    PageResponse<TagResp> list(String keyword, int limit, String cursor);

    /**
     * Replaces all tags associated with a specific post.
     * This operation atomically removes all existing post-tag associations
     * and creates new ones based on the provided tag names and IDs.
     * Tag names will be created if they don't exist (idempotent).
     * Only the post author can perform this operation.
     *
     * @param postId the ID of the post to update tags for
     * @param names optional list of tag names to associate (will be created if needed)
     * @param tagIds optional list of existing tag IDs to associate
     * @param operatorId the ID of the user performing the operation (must be post author)
     * @throws com.mobile.aura.support.BizException if post not found or operator is not the author
     */
    void replacePostTags(Long postId, List<String> names, List<Long> tagIds, Long operatorId);

    /**
     * Lists all tags associated with a specific post.
     * Tags are returned in alphabetical order by name.
     *
     * @param postId the ID of the post
     * @return list of tags associated with the post (empty list if none)
     */
    List<TagResp> listPostTags(Long postId);

    /**
     * Lists all public posts associated with a specific tag.
     * Results are paginated using cursor-based pagination and sorted by
     * creation time (newest first). Automatically filters out posts from
     * users who have blocked each other (bidirectional blocking).
     * Only includes posts with visibility=public and deleted_at=null.
     *
     * @param viewer the ID of the authenticated user (can be null for anonymous viewers)
     * @param tagId the ID of the tag
     * @param limit maximum number of posts to return (will be normalized to 1-100)
     * @param cursor optional pagination cursor in format "timestamp|id" (can be null)
     * @return paginated response containing post cards, next cursor, and hasMore flag
     */
    PageResponse<PostCardResp> listPostsByTag(Long viewer, Long tagId, int limit, String cursor);
}

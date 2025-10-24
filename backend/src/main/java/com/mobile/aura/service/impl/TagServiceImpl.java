package com.mobile.aura.service.impl;

import com.mobile.aura.domain.content.Post;
import com.mobile.aura.domain.content.PostMedia;
import com.mobile.aura.domain.content.PostTag;
import com.mobile.aura.domain.content.Tag;
import com.mobile.aura.dto.Cursor;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.post.PostCardResp;
import com.mobile.aura.dto.tag.TagDtos.*;
import com.mobile.aura.mapper.*;
import com.mobile.aura.service.TagService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of TagService using MyBatis XML mappers.
 * Handles all tag-related business operations including CRUD and post associations.
 * <p>
 * This implementation follows Rich Domain Model principles with no if statements,
 * no setters, and no builders in the service layer.
 */
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final PostTagMapper postTagMapper;
    private final PostMapper postMapper;
    private final PostMediaMapper mediaMapper;
    private final UserBlockMapper blockMapper;

    /* --------------------- Utility Methods --------------------- */

    /**
     * Check if two users have blocked each other (either direction).
     *
     * @param a first user ID
     * @param b second user ID
     * @return true if either user has blocked the other
     */
    private boolean blockedEither(Long a, Long b) {
        return Optional.ofNullable(a)
                .flatMap(userA -> Optional.ofNullable(b)
                        .map(userB -> blockMapper.existsEither(userA, userB) > 0))
                .orElse(false);
    }

    /**
     * Retrieve a tag and ensure it's modifiable.
     * Checks existence and throws exception if not found.
     *
     * @param tagId the ID of the tag
     * @return the tag
     * @throws BizException if tag not found
     */
    private Tag mustExistingTag(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        Tag.ensureExists(tag);
        return tag;
    }

    /**
     * Retrieve a post and ensure it's modifiable by the operator.
     * Checks existence, deletion status, and authorization.
     *
     * @param postId     the ID of the post
     * @param operatorId the ID of the operator
     * @throws BizException if post not found or user not authorized
     */
    private void mustModifiablePost(Long postId, Long operatorId) {
        Post post = postMapper.selectById(postId);
        post.ensureModifiable(operatorId);
    }

    /**
     * Find or create a tag by name (idempotent operation).
     * Uses case-insensitive lookup to avoid duplicate tags.
     *
     * @param name the tag name
     * @return the existing or newly created tag
     */
    private Tag findOrCreateTag(String name) {
        Tag tag = Tag.create(name);

        return Optional.ofNullable(tagMapper.findByNameLc(tag.getNameLc()))
                .orElseGet(() -> {
                    tagMapper.insert(tag);
                    // Handle potential race condition with unique constraint
                    return Optional.ofNullable(tagMapper.findByNameLc(tag.getNameLc()))
                            .orElse(tag);
                });
    }

    /**
     * Insert a post-tag association, ignoring duplicates.
     *
     * @param postId the ID of the post
     * @param tagId the ID of the tag
     */
    private void insertPostTagIgnoreDuplicate(Long postId, Long tagId) {
        PostTag postTag = PostTag.create(postId, tagId);
        try {
            postTagMapper.insert(postTag);
        } catch (Exception e) {
            // Ignore duplicate key exceptions
        }
    }

    /* --------------------- CRUD Operations --------------------- */

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Long create(TagCreateReq req) {
        Tag tag = findOrCreateTag(req.getName());
        return tag.getId();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void update(Long tagId, TagUpdateReq req) {
        Tag tag = mustExistingTag(tagId);

        Optional.ofNullable(req.getName())
                .ifPresent(tag::updateName);

        tagMapper.updateById(tag);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(Long tagId) {
        // Delete all post-tag associations first, then delete the tag
        tagMapper.deletePostTagsByTagId(tagId);
        int deletedCount = tagMapper.deleteById(tagId);
        Tag.ensureDeleted(deletedCount);
    }

    /** {@inheritDoc} */
    @Override
    public PageResponse<TagResp> list(String keyword, int limit, String cursor) {
        // Normalize keyword for case-insensitive search
        String normalizedKeyword = Optional.ofNullable(keyword)
                .filter(k -> !k.isBlank())
                .map(String::trim)
                .map(String::toLowerCase)
                .orElse(null);

        // Normalize cursor
        String normalizedCursor = Optional.ofNullable(cursor)
                .filter(c -> !c.isBlank())
                .orElse(null);

        List<Tag> tags = tagMapper.listTags(normalizedKeyword, normalizedCursor, limit + 1);

        return Tag.toPageResponse(tags, limit);
    }

    /* --------------------- Post-Tag Association Operations --------------------- */

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void replacePostTags(Long postId, List<String> names, List<Long> tagIds, Long operatorId) {
        mustModifiablePost(postId, operatorId);

        // Collect all tag IDs from both names and direct IDs
        Set<Long> finalTagIds = new LinkedHashSet<>();

        // Process tag names - find or create tags
        Optional.ofNullable(names)
                .orElse(List.of())
                .stream()
                .map(this::findOrCreateTag)
                .map(Tag::getId)
                .forEach(finalTagIds::add);

        // Add direct tag IDs
        Optional.ofNullable(tagIds)
                .ifPresent(finalTagIds::addAll);

        // Replace all post-tag associations
        tagMapper.deletePostTagsByPostId(postId);
        finalTagIds.forEach(tagId -> insertPostTagIgnoreDuplicate(postId, tagId));
    }

    /** {@inheritDoc} */
    @Override
    public List<TagResp> listPostTags(Long postId) {
        return tagMapper.findTagsByPostId(postId).stream()
                .map(Tag::toTagResp)
                .toList();
    }

    /* --------------------- Tag-Post Listing Operations --------------------- */

    /** {@inheritDoc} */
    @Override
    public PageResponse<PostCardResp> listPostsByTag(Long viewer, Long tagId, int limit, String cursor) {
        Cursor parsedCursor = Cursor.parse(cursor);
        List<Post> posts = tagMapper.listPostsByTagId(tagId, parsedCursor.getTimestamp(), parsedCursor.getId(), limit + 1);

        // Filter out posts from blocked users
        List<Post> filteredPosts = Optional.ofNullable(viewer)
                .map(v -> posts.stream()
                        .filter(post -> !blockedEither(v, post.getAuthorId()))
                        .toList())
                .orElse(posts);

        return Post.toCardsPageResponse(
                filteredPosts,
                limit,
                postId -> Optional.ofNullable(mediaMapper.findFirstByPostId(postId))
                        .map(PostMedia::getUrl)
                        .orElse(null)
        );
    }
}

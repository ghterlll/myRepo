package com.mobile.aura.service.impl;

import com.mobile.aura.domain.content.*;
import com.mobile.aura.domain.event.EventLog.EventType;
import com.mobile.aura.domain.user.UserFollow;
import com.mobile.aura.domain.user.UserSocialStats;
import com.mobile.aura.dto.Cursor;
import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.post.*;
import com.mobile.aura.dto.tag.TagDtos;
import com.mobile.aura.mapper.*;
import com.mobile.aura.service.EventLogService;
import com.mobile.aura.service.PostService;
import com.mobile.aura.service.TagService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of PostService using MyBatis Plus.
 * Handles all post-related business operations including CRUD, interactions, and comments.
 * <p>
 * This implementation follows Rich Domain Model principles with no if statements,
 * no setters, and no builders in the service layer.
 */
@Service
@RequiredArgsConstructor
public class PostServiceMPImpl implements PostService {

    private final PostMapper postMapper;
    private final PostStatisticsMapper statisticsMapper;
    private final PostMediaMapper mediaMapper;
    private final PostLikeMapper likeMapper;
    private final PostBookmarkMapper bookmarkMapper;
    private final PostCommentMapper commentMapper;

    private final UserMapper userMapper;
    private final UserFollowMapper followMapper;
    private final UserBlockMapper blockMapper;
    private final UserSocialStatsMapper socialStatsMapper;

    private final TagService tagService;
    private final EventLogService eventLogService;

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
     * Retrieve a post and ensure it's modifiable by the user.
     * Checks existence, deletion status, and authorization.
     *
     * @param postId the ID of the post
     * @param userId the ID of the user
     * @return the modifiable post
     * @throws BizException if post not found or user not authorized
     */
    private Post mustModifiablePost(Long postId, Long userId) {
        Post p = postMapper.selectById(postId);
        p.ensureModifiable(userId);
        return p;
    }

    /**
     * Retrieve a post and ensure it's readable by the viewer.
     * Checks deletion status, visibility, and block status.
     *
     * @param viewer the ID of the viewer
     * @param postId the ID of the post
     * @return the readable post
     * @throws BizException if post not found or not accessible
     */
    private Post mustReadablePost(Long viewer, Long postId) {
        Post p = postMapper.selectById(postId);
        p.ensureReadableBy(viewer, this::blockedEither);
        return p;
    }

    /* --------------------- Post CRUD & List Operations --------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Long create(Long authorId, PostCreateReq req) {
        // Create post using domain factory method
        Post p = Post.create(authorId, req);
        postMapper.insert(p);

        // Create initial statistics record
        PostStatistics stats = PostStatistics.createForPost(p.getId());
        statisticsMapper.insert(stats);

        // Handle media items if provided
        AtomicInteger sortOrder = new AtomicInteger(0);
        Optional.ofNullable(req.getMedias())
                .filter(medias -> !medias.isEmpty())
                .ifPresent(medias -> medias.stream()
                        .map(media -> PostMedia.createFrom(p.getId(), media, sortOrder.getAndIncrement()))
                        .forEach(mediaMapper::insert));

        // Handle tags if provided
        Optional.ofNullable(req.getTags())
                .filter(tags -> !tags.isEmpty())
                .ifPresent(tags -> tagService.replacePostTags(p.getId(), tags, null, authorId));

        // Update user's post count
        UserSocialStats.ensureUpdated(socialStatsMapper.incPostCount(authorId, +1));

        return p.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void update(Long postId, Long authorId, PostUpdateReq req) {
        Post post = mustModifiablePost(postId, authorId);

        // Update fields using domain methods
        post.updateTitle(req.getTitle());
        post.updateCaption(req.getCaption());

        postMapper.updateById(post);

        // Handle tags if provided
        Optional.ofNullable(req.getTags())
                .ifPresent(tags -> tagService.replacePostTags(postId, tags, null, authorId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void replaceMedia(Long postId, Long authorId, List<MediaItem> medias) {
        Post post = mustModifiablePost(postId, authorId);

        // Delete existing media
        mediaMapper.deleteByPostId(postId);

        // Insert new media using domain factory method
        AtomicInteger sortOrder = new AtomicInteger(0);
        Optional.ofNullable(medias)
                .orElse(List.of())
                .stream()
                .map(media -> PostMedia.createFrom(postId, media, sortOrder.getAndIncrement()))
                .forEach(mediaMapper::insert);

        // Update media count using domain method
        post.updateMediaCount(Optional.ofNullable(medias).map(List::size).orElse(0));
        postMapper.updateById(post);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void publish(Long postId, Long authorId) {
        Post post = mustModifiablePost(postId, authorId);
        post.publish();
        postMapper.updateById(post);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void hide(Long postId, Long authorId) {
        Post post = mustModifiablePost(postId, authorId);
        post.hide();
        postMapper.updateById(post);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void delete(Long postId, Long authorId) {
        // Idempotent delete using Optional
        Optional.ofNullable(postMapper.selectById(postId))
                .filter(post -> !post.isDeleted())
                .ifPresent(post -> {
                    post.ensureModifiable(authorId);
                    post.delete();
                    postMapper.updateById(post);

                    // Update user's post count
                    UserSocialStats.ensureUpdated(socialStatsMapper.incPostCount(authorId, -1));
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PostDetailResp detail(Long viewer, Long postId) {
        Post p = mustReadablePost(viewer, postId);

        // Log click event (user viewed post detail)
        if (viewer != null) {
            eventLogService.logEventAsync(viewer, postId, EventType.CLICK);
        }

        // Fetch media items
        List<PostMedia> mds = mediaMapper.listByPostId(postId);
        List<MediaItem> medias = mds.stream().map(Post::toMediaItem).toList();

        // Fetch tags
        List<String> tags = tagService.listPostTags(postId).stream()
                .map(TagDtos.TagResp::getName)
                .toList();

        // Convert to response DTO using domain method
        return p.toDetailResp(medias, tags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<PostCardResp> listPublic(Long viewer, int limit, String cursor) {
        Cursor parsedCursor = Cursor.parse(cursor);
        List<Post> posts = postMapper.listPublic(parsedCursor.getTimestamp(), parsedCursor.getId(), limit + 1);

        return Post.toCardsPageResponse(
                toPostsFilteringBlocks(viewer, posts),
                limit,
                postId -> Optional.ofNullable(mediaMapper.findFirstByPostId(postId))
                        .map(PostMedia::getUrl)
                        .orElse(null),
                authorId -> Optional.ofNullable(userMapper.selectById(authorId))
                        .map(user -> user.getNickname())
                        .orElse("Unknown")
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<PostCardResp> listFollowFeed(Long viewer, int limit, String cursor) {
        // Get list of users the viewer follows
        List<Long> followeeIds = followMapper.listByFollowerId(viewer)
                .stream()
                .map(UserFollow::getFolloweeId)
                .toList();

        return Optional.of(followeeIds)
                .filter(ids -> !ids.isEmpty())
                .map(ids -> {
                    Cursor parsedCursor = Cursor.parse(cursor);
                    List<Post> posts = postMapper.listFollowFeed(ids, parsedCursor.getTimestamp(), parsedCursor.getId(), limit + 1);

                    return Post.toCardsPageResponse(
                            toPostsFilteringBlocks(viewer, posts),
                            limit,
                            postId -> Optional.ofNullable(mediaMapper.findFirstByPostId(postId))
                                    .map(PostMedia::getUrl)
                                    .orElse(null),
                            authorId -> Optional.ofNullable(userMapper.selectById(authorId))
                                    .map(user -> user.getNickname())
                                    .orElse("Unknown")
                    );
                })
                .orElse(PageResponse.empty());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<PostCardResp> searchPublic(Long viewer, String keyword, String category, int limit, String cursor) {
        Cursor parsedCursor = Cursor.parse(cursor);
        List<Post> posts = postMapper.searchPublic(keyword, category, parsedCursor.getTimestamp(), parsedCursor.getId(), limit + 1);

        return Post.toCardsPageResponse(
                toPostsFilteringBlocks(viewer, posts),
                limit,
                postId -> Optional.ofNullable(mediaMapper.findFirstByPostId(postId))
                        .map(PostMedia::getUrl)
                        .orElse(null),
                authorId -> Optional.ofNullable(userMapper.selectById(authorId))
                        .map(user -> user.getNickname())
                        .orElse("Unknown")
        );
    }

    /**
     * Filter posts to exclude blocked users.
     *
     * @param viewer the ID of the viewer
     * @param list list of posts to filter
     * @return filtered list of posts
     */
    private List<Post> toPostsFilteringBlocks(Long viewer, List<Post> list) {
        return list.stream()
                .filter(post -> !blockedEither(viewer, post.getAuthorId()))
                .toList();
    }


    /* --------------------- Like/Bookmark Operations --------------------- */
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void like(Long uid, Long postId) {
        mustReadablePost(uid, postId);

        // Ensure not already liked
        PostLike.ensureNotExists(likeMapper.countLike(uid, postId));

        // Create and insert like using domain factory
        PostLike like = PostLike.create(uid, postId);
        likeMapper.insert(like);

        // Increment like count in statistics table
        PostStatistics.ensureUpdated(statisticsMapper.incLikeCount(postId, 1));

        // Log like event
        eventLogService.logEventAsync(uid, postId, EventType.LIKE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void unlike(Long uid, Long postId) {
        // Delete like and ensure it existed
        int deletedCount = likeMapper.deleteLike(uid, postId);
        PostLike.ensureDeleted(deletedCount);

        // Decrement like count in statistics table
        PostStatistics.ensureUpdated(statisticsMapper.incLikeCount(postId, -1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void bookmark(Long uid, Long postId) {
        mustReadablePost(uid, postId);

        // Ensure not already bookmarked
        PostBookmark.ensureNotExists(bookmarkMapper.countBookmark(uid, postId));

        // Create and insert bookmark using domain factory
        PostBookmark bookmark = PostBookmark.create(uid, postId);
        bookmarkMapper.insert(bookmark);

        // Increment bookmark count in statistics table
        PostStatistics.ensureUpdated(statisticsMapper.incBookmarkCount(postId, 1));

        // Log favorite event
        eventLogService.logEventAsync(uid, postId, EventType.FAVORITE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void unbookmark(Long uid, Long postId) {
        // Delete bookmark and ensure it existed
        int deletedCount = bookmarkMapper.deleteBookmark(uid, postId);
        PostBookmark.ensureDeleted(deletedCount);

        // Decrement bookmark count in statistics table
        PostStatistics.ensureUpdated(statisticsMapper.incBookmarkCount(postId, -1));
    }

    /* --------------------- Comment Operations (Nested Comments) --------------------- */

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public Long createComment(Long uid, Long postId, CommentCreateReq req) {
        mustReadablePost(uid, postId);

        Long commentId = Optional.ofNullable(req.getParentId())
                .map(parentId -> createReplyComment(uid, postId, parentId, req.getContent()))
                .orElseGet(() -> createRootComment(uid, postId, req.getContent()));

        // Log comment event
        eventLogService.logEventAsync(uid, postId, EventType.COMMENT);

        return commentId;
    }

    /**
     * Create a root-level comment.
     */
    private Long createRootComment(Long uid, Long postId, String content) {
        PostComment comment = PostComment.createRoot(postId, uid, content);
        commentMapper.insert(comment);
        comment.becomeRoot();
        commentMapper.updateById(comment);

        PostStatistics.ensureUpdated(statisticsMapper.incCommentCount(postId, 1));
        return comment.getId();
    }

    /**
     * Create a reply comment.
     */
    private Long createReplyComment(Long uid, Long postId, Long parentId, String content) {
        PostComment parent = commentMapper.selectById(parentId);
        PostComment reply = PostComment.createReply(postId, uid, parent, content, this::blockedEither);
        commentMapper.insert(reply);

        PostStatistics.ensureUpdated(statisticsMapper.incCommentCount(postId, 1));
        PostComment.ensureUpdated(commentMapper.incReplyCount(parent.getId(), 1));
        return reply.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteComment(Long uid, Long commentId) {
        Optional.ofNullable(commentMapper.selectById(commentId))
                .filter(comment -> !comment.isDeleted())
                .flatMap(comment -> Optional.ofNullable(postMapper.selectById(comment.getPostId()))
                        .map(post -> Map.entry(comment, post)))
                .filter(entry -> entry.getKey().canBeDeletedBy(uid, entry.getValue().getAuthorId()))
                .ifPresent(entry -> {
                    PostComment comment = entry.getKey();
                    comment.delete();
                    commentMapper.updateById(comment);

                    PostStatistics.ensureUpdated(statisticsMapper.incCommentCount(comment.getPostId(), -1));

                    Optional.ofNullable(comment.getParentId())
                            .ifPresent(parentId -> PostComment.ensureUpdated(commentMapper.incReplyCount(parentId, -1)));
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<CommentThreadResp> listRootComments(Long viewer, Long postId, int limit, String cursor, int previewSize) {
        mustReadablePost(viewer, postId);

        Cursor parsedCursor = Cursor.parse(cursor);
        List<PostComment> rootComments = commentMapper.listRootComments(postId, parsedCursor.formatTimestamp(), parsedCursor.getId(), limit + 1);

        return PostComment.toThreadsPageResponse(
                rootComments,
                limit,
                rootId -> commentMapper.listPreviewReplies(postId, rootId, Math.max(0, previewSize))
                        .stream()
                        .map(PostComment::toCommentResp)
                        .toList()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResponse<CommentResp> listReplies(Long viewer, Long rootCommentId, int limit, String cursor) {
        PostComment root = commentMapper.selectById(rootCommentId);
        root.ensureExists();
        mustReadablePost(viewer, root.getPostId());

        Cursor parsedCursor = Cursor.parse(cursor);
        List<PostComment> replies = commentMapper.listReplies(root.getPostId(), root.getId(), parsedCursor.formatTimestamp(), parsedCursor.getId(), limit + 1);

        return PostComment.toRepliesPageResponse(replies, limit);
    }
}

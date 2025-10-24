package com.mobile.aura.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mobile.aura.domain.content.Post;
import com.mobile.aura.domain.content.Tag;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MyBatis mapper for Tag entity.
 * Provides database access for tag operations using XML-based queries.
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    /**
     * Finds a tag by its lowercase normalized name for case-insensitive lookup.
     *
     * @param nameLc the lowercase name to search for
     * @return the Tag if found, null otherwise
     */
    Tag findByNameLc(@Param("nameLc") String nameLc);

    /**
     * Lists tags with optional keyword filtering and cursor-based pagination.
     *
     * @param keyword optional search keyword (filters by name_lc LIKE)
     * @param cursor optional pagination cursor (name_lc > cursor)
     * @param limit maximum number of results to return
     * @return list of tags matching the criteria
     */
    List<Tag> listTags(@Param("keyword") String keyword,
                       @Param("cursor") String cursor,
                       @Param("limit") int limit);

    /**
     * Finds all tags associated with a specific post.
     *
     * @param postId the ID of the post
     * @return list of tags associated with the post
     */
    List<Tag> findTagsByPostId(@Param("postId") Long postId);

    /**
     * Lists all public posts associated with a specific tag with cursor-based pagination.
     *
     * @param tagId the ID of the tag
     * @param cursorTimestamp optional cursor timestamp for pagination
     * @param cursorId optional cursor ID for pagination
     * @param limit maximum number of results to return
     * @return list of posts associated with the tag
     */
    List<Post> listPostsByTagId(@Param("tagId") Long tagId,
                                 @Param("cursorTimestamp") LocalDateTime cursorTimestamp,
                                 @Param("cursorId") Long cursorId,
                                 @Param("limit") int limit);

    /**
     * Deletes all post-tag associations for a given tag.
     *
     * @param tagId the ID of the tag
     */
    void deletePostTagsByTagId(@Param("tagId") Long tagId);

    /**
     * Deletes all post-tag associations for a given post.
     *
     * @param postId the ID of the post
     */
    void deletePostTagsByPostId(@Param("postId") Long postId);
}

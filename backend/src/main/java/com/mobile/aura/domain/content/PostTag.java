package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * PostTag domain entity representing the many-to-many relationship between posts and tags.
 * This is a simple association entity with factory method for controlled instantiation.
 */
@Data
@NoArgsConstructor
@TableName("post_tags")
public class PostTag {

    // Setters required by MyBatis Plus for query results population
    private Long postId;
    private Long tagId;

    /**
     * Private constructor for controlled instantiation through factory methods.
     *
     * @param postId the ID of the associated post
     * @param tagId the ID of the associated tag
     */
    private PostTag(Long postId, Long tagId) {
        this.postId = postId;
        this.tagId = tagId;
    }

    /* --------------------- Factory Methods --------------------- */

    /**
     * Creates a new post-tag association.
     *
     * @param postId the ID of the post
     * @param tagId the ID of the tag
     * @return a new PostTag instance ready for persistence
     */
    public static PostTag create(Long postId, Long tagId) {
        return new PostTag(postId, tagId);
    }


}

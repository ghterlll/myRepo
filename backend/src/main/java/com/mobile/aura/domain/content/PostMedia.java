package com.mobile.aura.domain.content;

import com.baomidou.mybatisplus.annotation.*;
import com.mobile.aura.dto.post.MediaItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * PostMedia domain model.
 * Represents a media item attached to a post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("post_media")
public class PostMedia {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private String mediaType;
    private String objectKey;
    private String url;
    private Integer width;
    private Integer height;
    private Integer sortOrder;
    private String blurhash;
    private String checksum;
    private Integer bytes;
    private String mimeType;
    private LocalDateTime createdAt;

    /**
     * Factory method to create a PostMedia from MediaItem and postId.
     *
     * @param postId the post ID
     * @param mediaItem the media item
     * @param sortOrder the sort order
     * @return new PostMedia instance
     */
    public static PostMedia createFrom(Long postId, MediaItem mediaItem, int sortOrder) {
        // Determine media type from MIME type, fallback to "image"
        String mediaType = Optional.ofNullable(mediaItem.getMimeType())
                .filter(mime -> mime.startsWith("video/"))
                .map(mime -> "video")
                .orElse("image");

        return new PostMedia(
                null,
                postId,
                mediaType,
                mediaItem.getObjectKey(),      // Store S3 key for deletion
                mediaItem.getUrl(),
                mediaItem.getWidth(),
                mediaItem.getHeight(),
                Optional.ofNullable(mediaItem.getSortOrder()).orElse(sortOrder),
                mediaItem.getBlurhash(),       // Progressive loading support
                mediaItem.getChecksum(),       // Deduplication support
                mediaItem.getBytes(),          // Quota management
                mediaItem.getMimeType(),       // Content type
                LocalDateTime.now()
        );
    }
}

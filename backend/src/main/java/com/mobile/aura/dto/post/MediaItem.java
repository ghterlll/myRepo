package com.mobile.aura.dto.post;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a media item (image, video, etc.) in a post.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaItem {

    @NotBlank(message = "Media URL is required")
    private String url;

    @Min(value = 1, message = "Width must be at least 1")
    private Integer width;

    @Min(value = 1, message = "Height must be at least 1")
    private Integer height;

    private Integer sortOrder;

    // Storage metadata - required for file management
    private String objectKey;    // S3 object key for deletion/management
    private String mimeType;      // MIME type (e.g., "image/jpeg", "video/mp4")
    private Integer bytes;        // File size in bytes for quota management

    // Optional enhancement fields
    private String checksum;      // File checksum (MD5/SHA256) for deduplication
    private String blurhash;      // BlurHash for progressive image loading
}

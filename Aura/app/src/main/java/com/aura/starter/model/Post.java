package com.aura.starter.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Post model for frontend display
 *
 * Note: Field naming conventions
 * - imageUri: Generic field name that can hold various image sources:
 *   * HTTP/HTTPS URLs (from MinIO or web)
 *   * Local file paths
 *   * Asset names
 *   * Resource IDs
 * - This corresponds to 'coverUrl' in backend PostCardResponse
 * - GlideUtils.loadImage() handles automatic source detection
 */
public class Post implements Serializable {
    public long createdAt;
    public String id, author, title, content, tags;
    public int likes = 0;
    public boolean liked = false, bookmarked = false;

    /**
     * Image URI - Can be HTTP URL, file path, asset name, or resource ID
     * Maps to backend PostCardResponse.coverUrl
     */
    public String imageUri;

    public List<String> comments = new ArrayList<>();

    public Post(String id, String author, String title, String content, String tags, String imageUri) {
        this.createdAt = System.currentTimeMillis();
        this.id=id; this.author=author; this.title=title; this.content=content; this.tags=tags; this.imageUri=imageUri;
    }
}

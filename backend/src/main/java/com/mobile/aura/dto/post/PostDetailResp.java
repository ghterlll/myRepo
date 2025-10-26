package com.mobile.aura.dto.post;


import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PostDetailResp {

    private Long id;
    private Long authorId;
    private String title;
    private String caption;
    private String status;
    private List<String> tags;
    private List<MediaItem> medias;
    private String createdAt;
    private String updatedAt;

    // Statistics
    private Integer likeCount;
    private Integer commentCount;
    private Integer bookmarkCount;

    public PostDetailResp(Long id, Long authorId, String title, String caption, String status, List<String> tags, List<MediaItem> medias, String createdAt, String updatedAt) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.caption = caption;
        this.status = status;
        this.tags = tags;
        this.medias = medias;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public PostDetailResp(Long id, Long authorId, String title, String caption, String status, List<String> tags, List<MediaItem> medias, String createdAt, String updatedAt, Integer likeCount, Integer commentCount, Integer bookmarkCount) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.caption = caption;
        this.status = status;
        this.tags = tags;
        this.medias = medias;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.bookmarkCount = bookmarkCount;
    }
}

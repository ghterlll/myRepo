package com.mobile.aura.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostCardResp {
    private Long id;
    private String coverUrl;
    private Long authorId;
    private String title;
    private String createdAt;

    // Statistics
    private Integer likeCount;
    private Integer commentCount;
    private Integer bookmarkCount;

    public PostCardResp(Long id, String coverUrl, Long authorId, String title, String createdAt) {
        this.id = id;
        this.coverUrl = coverUrl;
        this.authorId = authorId;
        this.title = title;
        this.createdAt = createdAt;
    }
}

package com.mobile.aura.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentResp {
    private Long id;
    private Long postId;
    private Long authorId;
    private Long rootId;
    private Long parentId;
    private String content;
    private String createdAt;
    private Integer replyCount;
}

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
    private String authorNickname;
    private String title;
    private String createdAt;
}

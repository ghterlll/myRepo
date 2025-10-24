package com.mobile.aura.dto.post;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CommentThreadResp {
    private CommentResp root;
    private List<CommentResp> replies;
}

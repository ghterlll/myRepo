package com.mobile.aura.controller;

import com.mobile.aura.dto.PageResponse;
import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.post.PostCardResp;
import com.mobile.aura.dto.tag.TagDtos.*;
import com.mobile.aura.dto.tag.TagListReq;
import com.mobile.aura.dto.tag.TagPostsReq;
import com.mobile.aura.service.TagService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tag")
public class TagController {

    private final TagService tagService;

    @PostMapping
    public ResponseResult<?> create(@Valid @RequestBody TagCreateReq req) {
        return ResponseResult.success(Map.of("id", tagService.create(req)));
    }

    @PatchMapping("/{id}")
    public ResponseResult<?> update(@PathVariable Long id, @Valid @RequestBody TagUpdateReq req) {
        tagService.update(id, req);
        return ResponseResult.success();
    }

    @DeleteMapping("/{id}")
    public ResponseResult<?> delete(@PathVariable Long id) {
        tagService.delete(id);
        return ResponseResult.success();
    }

    @GetMapping
    public ResponseResult<PageResponse<TagResp>> list(@Valid @ModelAttribute TagListReq req) {
        return ResponseResult.success(tagService.list(req.getQ(), req.getLimit(), req.getCursor()));
    }

    /**
     * Replace all tags for a post
     * Tags can be specified by name (auto-created) or by ID
     */
    @PutMapping("/posts/{postId}/tags")
    public ResponseResult<?> replacePostTags(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
            @PathVariable Long postId,
            @Valid @RequestBody PostTagReplaceReq req) {
        tagService.replacePostTags(postId, req.getNames(), req.getTagIds(), me);
        return ResponseResult.success();
    }

    @GetMapping("/posts/{postId}/tags")
    public ResponseResult<?> listPostTags(@PathVariable Long postId) {
        return ResponseResult.success(Map.of("items", tagService.listPostTags(postId)));
    }

    @GetMapping("/{tagId}/posts")
    public ResponseResult<PageResponse<PostCardResp>> postsByTag(
            @RequestAttribute(value = JwtAuthInterceptor.ATTR_USER_ID, required = false) Long viewer,
            @PathVariable Long tagId,
            @Valid @ModelAttribute TagPostsReq req) {
        return ResponseResult.success(tagService.listPostsByTag(viewer, tagId, req.getLimit(), req.getCursor()));
    }
}

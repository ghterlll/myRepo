package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.relation.RelationListReq;
import com.mobile.aura.service.RelationService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/relation")
public class RelationController {

    private final RelationService relationService;

    @PostMapping("/users/{id}/follow")
    public ResponseResult<?> follow(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                    @PathVariable("id") Long target) {
        relationService.follow(me, target);
        return ResponseResult.success();
    }

    @DeleteMapping("/users/{id}/follow")
    public ResponseResult<?> unfollow(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                      @PathVariable("id") Long target) {
        relationService.unfollow(me, target);
        return ResponseResult.success();
    }

    @PostMapping("/users/{id}/block")
    public ResponseResult<?> block(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                   @PathVariable("id") Long target) {
        relationService.block(me, target);
        return ResponseResult.success();
    }

    @DeleteMapping("/users/{id}/block")
    public ResponseResult<?> unblock(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                     @PathVariable("id") Long target) {
        relationService.unblock(me, target);
        return ResponseResult.success();
    }

    @GetMapping("/me/followers")
    public ResponseResult<?> myFollowers(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                         @Valid @ModelAttribute RelationListReq req) {
        return ResponseResult.success(relationService.followers(me, me, req.getLimit(), req.getCursor()));
    }

    @GetMapping("/me/followings")
    public ResponseResult<?> myFollowings(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                          @Valid @ModelAttribute RelationListReq req) {
        return ResponseResult.success(relationService.followings(me, me, req.getLimit(), req.getCursor()));
    }

    @GetMapping("/me/blocks")
    public ResponseResult<?> myBlocks(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                      @Valid @ModelAttribute RelationListReq req) {
        return ResponseResult.success(relationService.blocks(me, req.getLimit(), req.getCursor()));
    }

    @GetMapping("/users/{id}/followers")
    public ResponseResult<?> followers(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long viewer,
                                       @PathVariable("id") Long owner,
                                       @Valid @ModelAttribute RelationListReq req) {
        return ResponseResult.success(relationService.followers(owner, viewer, req.getLimit(), req.getCursor()));
    }

    @GetMapping("/users/{id}/followings")
    public ResponseResult<?> followings(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long viewer,
                                        @PathVariable("id") Long owner,
                                        @Valid @ModelAttribute RelationListReq req) {
        return ResponseResult.success(relationService.followings(owner, viewer, req.getLimit(), req.getCursor()));
    }

    @GetMapping("/users/{id}/relation")
    public ResponseResult<?> relation(@RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long me,
                                      @PathVariable("id") Long target) {
        boolean following = relationService.isFollowing(me, target);
        boolean blocked   = relationService.isBlocked(me, target);
        boolean blockedBy = relationService.isBlockedBy(me, target);
        return ResponseResult.success(Map.of(
                "following", following,
                "blocked",   blocked,
                "blockedBy", blockedBy
        ));
    }
}

package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.food.*;
import com.mobile.aura.service.FoodService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/food")
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseResult<?> search(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
            @Valid FoodSearchReq req){
        return ResponseResult.success(foodService.search(uid, req));
    }

    @GetMapping("/{id}")
    public ResponseResult<?> detail(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
            @PathVariable Long id){
        return ResponseResult.success(foodService.detail(uid, id));
    }

    @PostMapping("/my/foods")
    public ResponseResult<?> createMy(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
            @RequestBody FoodCreateReq req){
        return ResponseResult.success(java.util.Map.of("id", foodService.createMy(uid, req)));
    }

    @PatchMapping("/my/foods/{id}")
    public ResponseResult<?> updateMy(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
            @PathVariable Long id, @RequestBody FoodUpdateReq req){
        foodService.updateMy(uid, id, req);
        return ResponseResult.success();
    }

    @DeleteMapping("/my/foods/{id}")
    public ResponseResult<?> deleteMy(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
            @PathVariable Long id){
        foodService.deleteMy(uid, id);
        return ResponseResult.success();
    }

    @GetMapping("/my")
    public ResponseResult<?> myList(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long uid,
            @Valid FoodListReq req){
        return ResponseResult.success(foodService.myList(uid, req));
    }
}

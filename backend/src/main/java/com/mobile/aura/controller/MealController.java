// file: src/main/java/com/example/demo2/controller/MealController.java
package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.dto.SingleDateReq;
import com.mobile.aura.dto.meal.MealAddFreeInputReq;
import com.mobile.aura.dto.meal.MealAddFromSourceReq;
import com.mobile.aura.dto.meal.MealEditReq;
import com.mobile.aura.service.MealService;
import com.mobile.aura.support.JwtAuthInterceptor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meal")
public class MealController {

    private final MealService mealService;

    /**
     * Add meal from existing food source (user custom food)
     * Only requires sourceId - all food details fetched from database
     */
    @PostMapping("/from-source")
    public ResponseResult<?> addFromSource(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody MealAddFromSourceReq req) {
        Long id = mealService.addFromSource(userId, req);
        return ResponseResult.success(Map.of("id", id));
    }

    /**
     * Add meal with free input (no food source)
     * Requires all meal details including name, calories, and image
     */
    @PostMapping("/free-input")
    public ResponseResult<?> addFreeInput(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @Valid @RequestBody MealAddFreeInputReq req) {
        Long id = mealService.addFreeInput(userId, req);
        return ResponseResult.success(Map.of("id", id));
    }

    /**
     * Edit an existing meal log
     * Updates only the provided fields - all fields are optional
     */
    @PutMapping("/{id}")
    public ResponseResult<?> edit(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @PathVariable Long id,
            @Valid @RequestBody MealEditReq req) {
        mealService.edit(userId, id, req);
        return ResponseResult.success();
    }

    @DeleteMapping("/{id}")
    public ResponseResult<?> delete(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @PathVariable Long id) {
        mealService.delete(userId, id);
        return ResponseResult.success();
    }

    @GetMapping("/day")
    public ResponseResult<?> daySummary(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam(required = false) String date) {
        var d = SingleDateReq.parseDateOrToday(date);
        return ResponseResult.success(mealService.daySummary(userId, d));
    }
}

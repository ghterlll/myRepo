package com.mobile.aura.controller;

import com.mobile.aura.dto.ResponseResult;
import com.mobile.aura.service.FileStorageService;
import com.mobile.aura.service.FileStorageService.FileUploadResult;
import com.mobile.aura.support.JwtAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * File upload and management controller
 * Thin orchestration layer - delegates to FileStorageService
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Upload user avatar
     */
    @PostMapping("/avatar")
    public ResponseResult<?> uploadAvatar(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam("file") MultipartFile file) {

        FileUploadResult result = fileStorageService.uploadAvatar(userId, file);
        return ResponseResult.success(result);
    }

    /**
     * Upload post image
     */
    @PostMapping("/post/image")
    public ResponseResult<?> uploadPostImage(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam("file") MultipartFile file) {

        FileUploadResult result = fileStorageService.uploadPostImage(userId, file);
        return ResponseResult.success(result);
    }

    /**
     * Upload post video
     */
    @PostMapping("/post/video")
    public ResponseResult<?> uploadPostVideo(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam("file") MultipartFile file) {

        FileUploadResult result = fileStorageService.uploadPostVideo(userId, file);
        return ResponseResult.success(result);
    }

    /**
     * Batch upload images for a post
     */
    @PostMapping("/post/images/batch")
    public ResponseResult<?> uploadPostImagesBatch(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam("files") MultipartFile[] files) {

        List<FileUploadResult> results = fileStorageService.uploadPostImagesBatch(userId, files);
        return ResponseResult.success(Map.of("files", results));
    }

    /**
     * Upload food item image
     */
    @PostMapping("/food/image")
    public ResponseResult<?> uploadFoodImage(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam("file") MultipartFile file) {

        FileUploadResult result = fileStorageService.uploadFoodImage(userId, file);
        return ResponseResult.success(result);
    }

    /**
     * Delete a file
     */
    @DeleteMapping
    public ResponseResult<?> deleteFile(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam String key) {

        fileStorageService.deleteFile(userId, key);
        return ResponseResult.success();
    }

    /**
     * Generate temporary presigned URL for private files
     */
    @GetMapping("/presigned-url")
    public ResponseResult<?> getPresignedUrl(
            @RequestAttribute(JwtAuthInterceptor.ATTR_USER_ID) Long userId,
            @RequestParam String key,
            @RequestParam(defaultValue = "60") int expirationMinutes) {

        String presignedUrl = fileStorageService.generatePresignedUrl(userId, key, expirationMinutes);
        return ResponseResult.success(Map.of("url", presignedUrl));
    }

    /**
     * Check if a file exists
     */
    @GetMapping("/exists")
    public ResponseResult<?> fileExists(@RequestParam String key) {
        boolean exists = fileStorageService.fileExists(key);
        return ResponseResult.success(Map.of("exists", exists));
    }
}

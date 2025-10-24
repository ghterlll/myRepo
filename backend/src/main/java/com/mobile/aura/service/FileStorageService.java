package com.mobile.aura.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service for file storage operations
 */
public interface FileStorageService {

    /**
     * Upload avatar
     */
    FileUploadResult uploadAvatar(Long userId, MultipartFile file);

    /**
     * Upload post image
     */
    FileUploadResult uploadPostImage(Long userId, MultipartFile file);

    /**
     * Upload post video
     */
    FileUploadResult uploadPostVideo(Long userId, MultipartFile file);

    /**
     * Batch upload post images
     */
    List<FileUploadResult> uploadPostImagesBatch(Long userId, MultipartFile[] files);

    /**
     * Upload food item image
     */
    FileUploadResult uploadFoodImage(Long userId, MultipartFile file);

    /**
     * Delete file
     */
    void deleteFile(Long userId, String key);

    /**
     * Generate presigned URL for temporary access
     */
    String generatePresignedUrl(Long userId, String key, int expirationMinutes);

    /**
     * Check if file exists
     */
    boolean fileExists(String key);

    /**
     * Result of file upload operation
     *
     * @param url Public URL of the uploaded file
     * @param key S3 object key for file management
     * @param mimeType MIME type (e.g., "image/jpeg", "video/mp4")
     * @param bytes File size in bytes
     */
    record FileUploadResult(String url, String key, String mimeType, Long bytes) {}
}

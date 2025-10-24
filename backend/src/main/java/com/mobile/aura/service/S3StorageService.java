package com.mobile.aura.service;

import java.io.InputStream;

/**
 * Service for S3-compatible object storage operations
 * Works with SeaweedFS, MinIO, AWS S3, or any S3-compatible storage
 */
public interface S3StorageService {

    /**
     * Upload a file to S3 storage
     *
     * @param key         Object key (file path in bucket)
     * @param inputStream File content stream
     * @param contentType MIME type (e.g., "image/jpeg", "video/mp4")
     * @param contentLength File size in bytes
     * @return Public URL of the uploaded file
     */
    String uploadFile(String key, InputStream inputStream, String contentType, long contentLength);

    /**
     * Delete a file from S3 storage
     *
     * @param key Object key (file path in bucket)
     */
    void deleteFile(String key);

    /**
     * Generate a presigned URL for temporary access to a private file
     *
     * @param key Object key (file path in bucket)
     * @param expirationMinutes URL expiration time in minutes
     * @return Presigned URL
     */
    String generatePresignedUrl(String key, int expirationMinutes);

    /**
     * Check if a file exists in S3 storage
     *
     * @param key Object key (file path in bucket)
     * @return true if file exists, false otherwise
     */
    boolean fileExists(String key);
}

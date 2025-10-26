package com.mobile.aura.domain.file;

import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.support.BizException;
import lombok.Getter;

import java.util.UUID;

/**
 * Domain model for file upload operations
 * Encapsulates file validation and path generation logic
 */
public class FileUpload {

    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024;      // 5MB
    private static final long MAX_IMAGE_SIZE = 100 * 1024 * 1024;     // 100MB

    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    private final Long userId;
    private final String originalFilename;
    @Getter
    private final String contentType;
    @Getter
    private final long fileSize;

    private FileUpload(Long userId, String originalFilename, String contentType, long fileSize) {
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
    }

    /**
     * Factory method for avatar upload
     */
    public static FileUpload forAvatar(Long userId, String filename, String contentType, long size) {
        FileUpload upload = new FileUpload(userId, filename, contentType, size);
        upload.validateSize(MAX_AVATAR_SIZE);
        upload.validateContentType(ALLOWED_IMAGE_TYPES);
        return upload;
    }

    /**
     * Factory method for post image upload
     */
    public static FileUpload forPostImage(Long userId, String filename, String contentType, long size) {
        FileUpload upload = new FileUpload(userId, filename, contentType, size);
        upload.validateSize(MAX_IMAGE_SIZE);
        upload.validateContentType(ALLOWED_IMAGE_TYPES);
        return upload;
    }

    /**
     * Factory method for food item image upload
     */
    public static FileUpload forFoodImage(Long userId, String filename, String contentType, long size) {
        FileUpload upload = new FileUpload(userId, filename, contentType, size);
        upload.validateSize(MAX_IMAGE_SIZE);
        upload.validateContentType(ALLOWED_IMAGE_TYPES);
        return upload;
    }

    /**
     * Generate storage key for avatar
     */
    public String generateAvatarKey() {
        return String.format("avatars/%d/%s-%s", userId, UUID.randomUUID(), sanitizeFilename());
    }

    /**
     * Generate storage key for post image
     */
    public String generatePostImageKey() {
        return String.format("posts/%d/images/%s-%s", userId, UUID.randomUUID(), sanitizeFilename());
    }

    /**
     * Generate storage key for post video
     */
    public String generatePostVideoKey() {
        return String.format("posts/%d/videos/%s-%s", userId, UUID.randomUUID(), sanitizeFilename());
    }

    /**
     * Generate storage key for food item image
     */
    public String generateFoodImageKey() {
        return String.format("foods/%d/%s-%s", userId, UUID.randomUUID(), sanitizeFilename());
    }

    /**
     * Verify user owns the file (for delete/access operations)
     */
    public static void verifyOwnership(Long userId, String key) {
        if (key == null || key.isBlank()) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }

        if (!key.startsWith("avatars/" + userId + "/") &&
            !key.startsWith("posts/" + userId + "/") &&
            !key.startsWith("foods/" + userId + "/")) {
            throw new BizException(CommonStatusEnum.FORBIDDEN);
        }
    }

    /**
     * Validate file size
     */
    private void validateSize(long maxSize) {
        if (fileSize == 0) {
            throw new BizException(CommonStatusEnum.FILE_EMPTY);
        }

        if (fileSize > maxSize) {
            throw new BizException(CommonStatusEnum.FILE_TOO_LARGE);
        }
    }

    /**
     * Validate content type
     */
    private void validateContentType(String[] allowedTypes) {
        if (contentType == null || contentType.isBlank()) {
            throw new BizException(CommonStatusEnum.FILE_TYPE_UNKNOWN);
        }

        for (String allowedType : allowedTypes) {
            if (contentType.equals(allowedType)) {
                return;
            }
        }

        throw new BizException(CommonStatusEnum.FILE_TYPE_NOT_ALLOWED);
    }

    /**
     * Sanitize filename to prevent path traversal and special characters
     */
    private String sanitizeFilename() {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "unnamed";
        }

        String filename = originalFilename;

        // Remove path separators
        filename = filename.replaceAll("[/\\\\]", "");

        // Remove or replace special characters
        filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

        // Limit filename length
        if (filename.length() > 100) {
            String extension = "";
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
                filename = filename.substring(0, Math.min(100 - extension.length(), dotIndex));
            } else {
                filename = filename.substring(0, 100);
            }
            filename = filename + extension;
        }

        return filename;
    }

}

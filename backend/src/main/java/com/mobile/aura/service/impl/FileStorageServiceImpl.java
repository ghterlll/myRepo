package com.mobile.aura.service.impl;

import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.domain.file.FileUpload;
import com.mobile.aura.service.FileStorageService;
import com.mobile.aura.service.S3StorageService;
import com.mobile.aura.support.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * File storage service implementation
 * Orchestrates domain logic and S3 operations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private static final int MAX_BATCH_SIZE = 9;

    private final S3StorageService s3StorageService;

    @Override
    public FileUploadResult uploadAvatar(Long userId, MultipartFile file) {
        try {
            // Domain model handles validation
            FileUpload upload = FileUpload.forAvatar(
                    userId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );

            String key = upload.generateAvatarKey();

            // Delegate to infrastructure layer
            String url = s3StorageService.uploadFile(
                    key,
                    file.getInputStream(),
                    upload.getContentType(),
                    upload.getFileSize()
            );

            log.info("Avatar uploaded for user {}: {}", userId, key);
            return new FileUploadResult(url, key, upload.getContentType(), upload.getFileSize());

        } catch (IOException e) {
            log.error("Failed to upload avatar for user {}", userId, e);
            throw new BizException(CommonStatusEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public FileUploadResult uploadPostImage(Long userId, MultipartFile file) {
        try {
            // Domain model handles validation
            FileUpload upload = FileUpload.forPostImage(
                    userId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );

            String key = upload.generatePostImageKey();

            // Delegate to infrastructure layer
            String url = s3StorageService.uploadFile(
                    key,
                    file.getInputStream(),
                    upload.getContentType(),
                    upload.getFileSize()
            );

            log.info("Post image uploaded for user {}: {}", userId, key);
            return new FileUploadResult(url, key, upload.getContentType(), upload.getFileSize());

        } catch (IOException e) {
            log.error("Failed to upload post image for user {}", userId, e);
            throw new BizException(CommonStatusEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public FileUploadResult uploadPostVideo(Long userId, MultipartFile file) {
        try {
            // Domain model handles validation
            FileUpload upload = FileUpload.forPostVideo(
                    userId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );

            String key = upload.generatePostVideoKey();

            // Delegate to infrastructure layer
            String url = s3StorageService.uploadFile(
                    key,
                    file.getInputStream(),
                    upload.getContentType(),
                    upload.getFileSize()
            );

            log.info("Post video uploaded for user {}: {}", userId, key);
            return new FileUploadResult(url, key, upload.getContentType(), upload.getFileSize());

        } catch (IOException e) {
            log.error("Failed to upload post video for user {}", userId, e);
            throw new BizException(CommonStatusEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public List<FileUploadResult> uploadPostImagesBatch(Long userId, MultipartFile[] files) {
        if (files.length > MAX_BATCH_SIZE) {
            throw new BizException(CommonStatusEnum.INVALID_PARAM);
        }

        List<FileUploadResult> results = new ArrayList<>();

        for (MultipartFile file : files) {
            FileUploadResult result = uploadPostImage(userId, file);
            results.add(result);
        }

        log.info("Batch uploaded {} images for user {}", files.length, userId);
        return results;
    }

    @Override
    public FileUploadResult uploadFoodImage(Long userId, MultipartFile file) {
        try {
            // Domain model handles validation
            FileUpload upload = FileUpload.forFoodImage(
                    userId,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );

            String key = upload.generateFoodImageKey();

            // Delegate to infrastructure layer
            String url = s3StorageService.uploadFile(
                    key,
                    file.getInputStream(),
                    upload.getContentType(),
                    upload.getFileSize()
            );

            log.info("Food image uploaded for user {}: {}", userId, key);
            return new FileUploadResult(url, key, upload.getContentType(), upload.getFileSize());

        } catch (IOException e) {
            log.error("Failed to upload food image for user {}", userId, e);
            throw new BizException(CommonStatusEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteFile(Long userId, String key) {
        // Domain model verifies ownership
        FileUpload.verifyOwnership(userId, key);

        // Delegate to infrastructure layer
        s3StorageService.deleteFile(key);

        log.info("File deleted by user {}: {}", userId, key);
    }

    @Override
    public String generatePresignedUrl(Long userId, String key, int expirationMinutes) {
        // Domain model verifies ownership
        FileUpload.verifyOwnership(userId, key);

        // Delegate to infrastructure layer
        return s3StorageService.generatePresignedUrl(key, expirationMinutes);
    }

    @Override
    public boolean fileExists(String key) {
        return s3StorageService.fileExists(key);
    }
}

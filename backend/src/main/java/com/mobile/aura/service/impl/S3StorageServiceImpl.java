package com.mobile.aura.service.impl;

import com.mobile.aura.config.S3Properties;
import com.mobile.aura.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;

/**
 * S3-compatible storage service implementation for MinIO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageServiceImpl implements S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String uploadFile(String key, InputStream inputStream, String contentType, long contentLength) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(inputStream, contentLength));

            log.info("File uploaded successfully: {}", key);
            return buildPublicUrl(key);
        } catch (S3Exception e) {
            log.error("Failed to upload file: {}", key, e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    @Override
    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully: {}", key);
        } catch (S3Exception e) {
            log.error("Failed to delete file: {}", key, e);
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    @Override
    public String generatePresignedUrl(String key, int expirationMinutes) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (S3Exception e) {
            log.error("Failed to generate presigned URL for: {}", key, e);
            throw new RuntimeException("Failed to generate presigned URL", e);
        }
    }

    @Override
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Failed to check file existence: {}", key, e);
            throw new RuntimeException("Failed to check file existence", e);
        }
    }

    /**
     * Build public URL for uploaded file
     */
    private String buildPublicUrl(String key) {
        return String.format("%s/%s/%s", s3Properties.getEndpoint(), s3Properties.getBucket(), key);
    }
}

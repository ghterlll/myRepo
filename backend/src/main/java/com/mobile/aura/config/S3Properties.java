package com.mobile.aura.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO configuration properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class S3Properties {

    /**
     * MinIO endpoint URL (e.g., http://localhost:9000)
     */
    private String endpoint;

    /**
     * Access key ID
     */
    private String accessKey;

    /**
     * Secret access key
     */
    private String secretKey;

    /**
     * Bucket name for storing files
     */
    private String bucketName;

    /**
     * Whether to use HTTPS
     */
    private boolean secure = false;

    /**
     * AWS region (can be any value for MinIO, e.g., "us-east-1")
     */
    private String region = "us-east-1";

    /**
     * Alias for bucket-name to maintain compatibility
     */
    public String getBucket() {
        return bucketName;
    }
}

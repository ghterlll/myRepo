package com.mobile.aura.domain.auth;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.constant.TokenConstants;
import com.mobile.aura.mapper.RefreshTokenMapper;
import com.mobile.aura.support.BizException;
import lombok.Data;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@TableName("auth_refresh_token")
public class RefreshToken {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String deviceId;
    private String tokenHash;
    private LocalDateTime expiresAt;

    /**
     * Normalize device ID, default to "web" if null or blank
     */
    public static String normalizeDeviceId(String deviceId) {
        return (deviceId == null || deviceId.isBlank()) ? "web" : deviceId;
    }

    /**
     * Generate SHA-256 hash of a token string
     */
    public static String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if token is expired
     */
    public boolean isExpired() {
        return this.expiresAt != null && this.expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Verify token exists and is not expired, throw exception otherwise
     */
    public static RefreshToken ensureValidOrThrow(RefreshToken token) {
        if (token == null || token.isExpired()) {
            throw new BizException(CommonStatusEnum.REFRESH_INVALID);
        }
        return token;
    }

    /**
     * Factory method to create a new refresh token for a user and device
     * Returns both the token entity and the raw token string
     */
    public static RefreshTokenPair createForUserDevice(Long userId, String deviceId, RefreshToken existingToken) {
        String normalizedDevice = normalizeDeviceId(deviceId);
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String hash = hashToken(rawToken);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setDeviceId(normalizedDevice);
        token.setTokenHash(hash);
        token.setExpiresAt(LocalDateTime.now().plusSeconds(TokenConstants.REFRESH_TTL_SEC));

        // If updating existing token, preserve the ID
        if (existingToken != null) {
            token.setId(existingToken.getId());
        }

        return new RefreshTokenPair(token, rawToken);
    }

    /**
     * Update this token with a new refresh token
     */
    public String rotateToken() {
        String newRawToken = UUID.randomUUID().toString().replace("-", "");
        this.tokenHash = hashToken(newRawToken);
        this.expiresAt = LocalDateTime.now().plusSeconds(TokenConstants.REFRESH_TTL_SEC);
        return newRawToken;
    }

    /**
     * Save or update this token in the database
     */
    public void saveOrUpdate(RefreshTokenMapper mapper) {
        if (this.id == null) {
            mapper.insert(this);
        } else {
            mapper.updateById(this);
        }
    }
}

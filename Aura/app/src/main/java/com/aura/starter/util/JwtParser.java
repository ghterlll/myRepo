package com.aura.starter.util;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Simple JWT parser to extract claims from JWT tokens
 * JWT format: header.payload.signature
 * We only need to decode the payload (middle part) to get userId
 */
public class JwtParser {
    private static final String TAG = "JwtParser";

    /**
     * Extract userId from JWT access token
     * @param token JWT token string
     * @return userId as Long, or null if parsing fails
     */
    public static Long extractUserId(String token) {
        if (token == null || token.isEmpty()) {
            Log.w(TAG, "Token is null or empty");
            return null;
        }

        try {
            // JWT format: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                Log.e(TAG, "Invalid JWT format - expected 3 parts, got " + parts.length);
                return null;
            }

            // Decode the payload (second part)
            String payload = parts[1];

            // JWT uses URL-safe Base64 encoding
            byte[] decodedBytes = Base64.decode(payload, Base64.URL_SAFE | Base64.NO_WRAP);
            String decodedPayload = new String(decodedBytes);

            Log.d(TAG, "Decoded JWT payload: " + decodedPayload);

            // Parse JSON payload
            JSONObject json = new JSONObject(decodedPayload);

            // Extract "sub" (subject) field which contains userId
            if (json.has("sub")) {
                String subjectStr = json.getString("sub");
                Long userId = Long.parseLong(subjectStr);
                Log.d(TAG, "Extracted userId from JWT: " + userId);
                return userId;
            } else {
                Log.w(TAG, "JWT payload does not contain 'sub' field");
                return null;
            }

        } catch (NumberFormatException e) {
            Log.e(TAG, "UserId parse error", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Base64 decode error", e);
            return null;
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error", e);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error parsing JWT", e);
            return null;
        }
    }

    /**
     * Extract device ID from JWT access token
     * @param token JWT token string
     * @return deviceId as String, or null if parsing fails
     */
    public static String extractDeviceId(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            byte[] decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP);
            String decodedPayload = new String(decodedBytes);

            JSONObject json = new JSONObject(decodedPayload);

            if (json.has("deviceId")) {
                return json.getString("deviceId");
            }
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error extracting deviceId from JWT", e);
            return null;
        }
    }

    /**
     * Extract expiration timestamp from JWT access token
     * @param token JWT token string
     * @return expiration time in seconds since epoch, or null if parsing fails
     */
    public static Long extractExpiration(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            byte[] decodedBytes = Base64.decode(parts[1], Base64.URL_SAFE | Base64.NO_WRAP);
            String decodedPayload = new String(decodedBytes);

            JSONObject json = new JSONObject(decodedPayload);

            if (json.has("exp")) {
                return json.getLong("exp");
            }
            return null;

        } catch (Exception e) {
            Log.e(TAG, "Error extracting expiration from JWT", e);
            return null;
        }
    }

    /**
     * Check if JWT token is expired
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    public static boolean isTokenExpired(String token) {
        Long exp = extractExpiration(token);
        if (exp == null) {
            return true; // Treat invalid tokens as expired
        }

        long nowSeconds = System.currentTimeMillis() / 1000;
        return nowSeconds >= exp;
    }
}

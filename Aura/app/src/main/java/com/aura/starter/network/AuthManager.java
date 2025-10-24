package com.aura.starter.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;

/**
 * 认证管理器 - 管理登录状态和Token
 */
public class AuthManager {
    private static final String PREF_NAME = "aura_auth";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_DEVICE_ID = "device_id";

    private final SharedPreferences prefs;
    private final Context context;

    public AuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Development mode disabled - require real login
        // if (getAccessToken() == null) {
        //     saveLoginInfo("fake_access_token_for_development", "fake_refresh_token", 1L);
        // }
    }

    /**
     * 保存登录信息
     */
    public void saveLoginInfo(String accessToken, String refreshToken, Long userId) {
        prefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .putLong(KEY_USER_ID, userId != null ? userId : 0L)
                .apply();
        
        // 更新ApiClient的token
        ApiClient.setAccessToken(accessToken);
    }

    /**
     * 获取访问令牌
     */
    public String getAccessToken() {
        return prefs.getString(KEY_ACCESS_TOKEN, null);
    }

    /**
     * 获取刷新令牌
     */
    public String getRefreshToken() {
        return prefs.getString(KEY_REFRESH_TOKEN, null);
    }

    /**
     * 获取用户ID
     */
    public Long getUserId() {
        long id = prefs.getLong(KEY_USER_ID, 0L);
        return id > 0 ? id : null;
    }

    /**
     * 获取设备ID
     */
    public String getDeviceId() {
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        if (deviceId == null) {
            deviceId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID
            );
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply();
        }
        return deviceId;
    }

    /**
     * 是否已登录
     */
    public boolean isLoggedIn() {
        return getAccessToken() != null && getUserId() != null;
    }

    /**
     * 登出
     */
    public void logout() {
        prefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .apply();
        
        ApiClient.setAccessToken(null);
    }

    /**
     * 初始化Token到ApiClient
     */
    public void initTokenToApiClient() {
        String token = getAccessToken();
        if (token != null) {
            ApiClient.setAccessToken(token);
        }
    }
}


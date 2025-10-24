package com.mobile.aura.constant;

public final class TokenConstants {
    private TokenConstants(){}
    public static final String TOKEN_PREFIX = "Bearer ";
    public static final long ACCESS_TTL_SEC  = 15 * 60;        // 15 min
    public static final long REFRESH_TTL_SEC = 30L * 24 * 3600; // 30 days
}

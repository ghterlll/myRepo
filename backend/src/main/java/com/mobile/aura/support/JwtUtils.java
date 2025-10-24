// file: com/example/demo2/support/JwtUtils.java
package com.mobile.aura.support;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;


public final class JwtUtils {
    private JwtUtils() {}

    private static final String SECRET = "a_very_long_demo_secret_change_me_256_bits!";

    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    public static String createAccess(Long userId, String deviceId, long ttlSec){
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of("deviceId", deviceId))
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(ttlSec)))
                .signWith(KEY)
                .compact();
    }

    public static Jws<Claims> parse(String token){
        return Jwts.parser()
                .verifyWith(KEY)
                .clockSkewSeconds(120)
                .build()
                .parseSignedClaims(token);
    }

    public static Long getUserId(Jws<Claims> jws){ return Long.valueOf(jws.getPayload().getSubject()); }
    public static String getDeviceId(Jws<Claims> jws){ return jws.getPayload().get("deviceId", String.class); }
}

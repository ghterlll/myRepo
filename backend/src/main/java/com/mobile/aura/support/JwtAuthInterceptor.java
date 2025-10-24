
package com.mobile.aura.support;

import com.alibaba.fastjson2.JSON;
import com.mobile.aura.constant.CommonStatusEnum;
import com.mobile.aura.dto.ResponseResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import lombok.extern.slf4j.Slf4j;

// support/JwtAuthInterceptor.java
@Slf4j
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {
    public static final String ATTR_USER_ID = "X-User-Id";
    public static final String ATTR_DEVICE  = "X-Device-Id";


    @Override
    public boolean preHandle(HttpServletRequest req, @NonNull HttpServletResponse resp, @NonNull Object handler) {
        String auth = req.getHeader("Authorization");
        log.info("[AUTH] {} {}", req.getMethod(), req.getRequestURI());
        log.info("[AUTH] Authorization: {}", auth);
        if (auth == null || auth.isBlank()) {
            write401(resp, CommonStatusEnum.UNAUTHORIZED); // 1102
            return false;
        }

        String token = auth.trim().replaceFirst("(?i)^bearer\\s+", "").trim();
        if (token.startsWith("\"") && token.endsWith("\"") && token.length() > 1) {
            token = token.substring(1, token.length() - 1);
        }

        try {
            var jws = JwtUtils.parse(token);
            req.setAttribute(ATTR_USER_ID, JwtUtils.getUserId(jws));
            req.setAttribute(ATTR_DEVICE,  JwtUtils.getDeviceId(jws));
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            write401(resp, CommonStatusEnum.TOKEN_EXPIRED);
            return false;
        } catch (Exception e) {
            write401(resp, CommonStatusEnum.UNAUTHORIZED);  // 1102：TOKEN INVALID/UNAUTHORIZED
            return false;
        }
    }

    private void write401(HttpServletResponse resp, CommonStatusEnum code) {
        try {
            resp.resetBuffer();
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            resp.setCharacterEncoding("UTF-8");
            resp.setContentType("application/json;charset=UTF-8");

            var body = ResponseResult.fail(code.getCode(), code.getValue());
            resp.getWriter().write(JSON.toJSONString(body));
            resp.flushBuffer();
        } catch (Exception ignore) {}
    }
}

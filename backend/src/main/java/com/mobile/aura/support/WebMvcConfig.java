// file: com/example/demo2/support/WebMvcConfig.java
package com.mobile.aura.support;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtAuthInterceptor jwt;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwt)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/auth/**",
                        "/users/register",
                        "/user/register",
                        "/user/auth/**",
                        "/error",
                        "/api/test/**",
                        "/test/**",                      // Test endpoints
                        "/api/v1/user/register",         // Official registration
                        "/api/v1/user/register/**",      // Registration sub-paths
                        "/api/v1/user/register/code",    // Registration code endpoint
                        "/api/v1/user/register/verify",  // Registration verify endpoint
                        "/api/v1/user/auth/**",          // Auth endpoints (login, refresh)
                        "/api/v1/user/login"             // Login alias
                );
    }
}

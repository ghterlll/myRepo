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
                        "/api/v1/user/register",         // Official registration
                        "/api/v1/user/register/**",      // Registration sub-paths
                        "/api/v1/user/auth/**",          // Auth endpoints (login, refresh)
                        "/api/v1/user/login",            // Login alias
                        "/test/**",                      // Test endpoints (development only)
                        "/swagger-ui/**",                // Swagger UI resources
                        "/swagger-ui.html",              // Swagger UI page
                        "/v3/api-docs/**",               // OpenAPI documentation
                        "/swagger-resources/**",         // Swagger resources
                        "/webjars/**"                    // Swagger UI dependencies
                );
    }
}

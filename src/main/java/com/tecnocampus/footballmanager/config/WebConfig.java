package com.tecnocampus.footballmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration so that an HTML page opened from disk (origin "null")
 * or served from a different local port (origin "http://localhost:5500", etc.)
 * can call this API.
 *
 * For a real production deployment, replace allowedOriginPatterns("*") with
 * the actual domain(s) that should be allowed.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*");
    }
}

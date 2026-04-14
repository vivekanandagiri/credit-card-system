package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

/**
 * Web MVC configuration class.
 *
 * <p>This configuration is responsible for:
 * <ul>
 *     <li>Registering application interceptors</li>
 *     <li>Customizing request handling behavior</li>
 * </ul>
 *
 * <p><b>Timezone Handling:</b>
 * <ul>
 *     <li>Registers {@link TimezoneInterceptor}</li>
 *     <li>Applies it to all API endpoints (/api/**)</li>
 *     <li>Ensures every request has a resolved timezone</li>
 * </ul>
 *
 * <p><b>Flow:</b>
 * <pre>
 * Incoming Request
 *        ↓
 * TimezoneInterceptor (reads X-Timezone header)
 *        ↓
 * TimezoneContext (ThreadLocal)
 *        ↓
 * Controller → Service → Response
 * </pre>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TimezoneInterceptor timezoneInterceptor;

    public WebConfig(TimezoneInterceptor timezoneInterceptor) {
        this.timezoneInterceptor = timezoneInterceptor;
    }

    /**
     * Registers application interceptors.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(timezoneInterceptor)
                .addPathPatterns("/api/**"); // Apply only to API endpoints
    }
}
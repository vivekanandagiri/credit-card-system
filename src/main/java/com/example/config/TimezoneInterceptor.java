package com.example.config;

import com.example.util.TimezoneContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.ZoneId;

/**
 * Interceptor for resolving and setting user timezone per request.
 *
 * <p><b>Flow:</b>
 * <ul>
 *     <li>Reads timezone from request header: <b>X-Timezone</b></li>
 *     <li>Validates and converts it to {@link ZoneId}</li>
 *     <li>Falls back to default timezone if invalid or missing</li>
 *     <li>Stores timezone in {@link TimezoneContext}</li>
 *     <li>Clears context after request completion</li>
 * </ul>
 *
 * <p><b>Example Header:</b>
 * <pre>
 * X-Timezone: Asia/Kolkata
 * </pre>
 *
 * <p><b>Important:</b>
 * Always clears ThreadLocal to avoid memory leaks in thread pools.
 */
@Component
public class TimezoneInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TimezoneInterceptor.class);

    private final TimezoneConfig timezoneConfig;

    public TimezoneInterceptor(TimezoneConfig timezoneConfig) {
        this.timezoneConfig = timezoneConfig;
    }

    /**
     * Pre-handle request to resolve timezone.
     */
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {

        String timezoneHeader = request.getHeader("X-Timezone");

        ZoneId zone;

        try {
            zone = (timezoneHeader != null && !timezoneHeader.isBlank())
                    ? ZoneId.of(timezoneHeader)
                    : timezoneConfig.getDefaultZone();

        } catch (Exception ex) {
            log.warn("Invalid timezone header: {}. Falling back to default.", timezoneHeader);
            zone = timezoneConfig.getDefaultZone();
        }

        // Set in ThreadLocal context
        TimezoneContext.setZone(zone);

        log.debug("Timezone set for request: {}", zone);

        return true;
    }

    /**
     * Clear timezone after request completion.
     */
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {

        TimezoneContext.clear();
        log.debug("Timezone cleared after request");
    }
}
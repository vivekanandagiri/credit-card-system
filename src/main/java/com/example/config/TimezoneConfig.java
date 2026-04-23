package com.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

import java.time.ZoneId;
/**
 * Provide Default Time zone
 * Configuration class for application default timezone.
 *
 * <p>Reads timezone from application properties:
 * <pre>
 * app.default-timezone=UTC
 * </pre>
 *
 * <p>If the provided timezone is invalid, falls back to UTC.
 *
 * <p><b>Usage:</b>
 * <ul>
 *     <li>Used by {@link com.example.config.TimezoneInterceptor}</li>
 *     <li>Fallback when request does not provide timezone</li>
 * </ul>
 *
 * <p><b>Best Practice:</b>
 * Always keep system default timezone as UTC for consistency.
 */
@Configuration
public class TimezoneConfig {

	@Value("${app.default-timezone:UTC}")
    private String timezone;

	private ZoneId defaultZone;


    /**
     * Initializes and validates timezone after bean creation.
     */
    @PostConstruct
    public void init() {
        try {
            defaultZone = ZoneId.of(timezone);
        } catch (Exception e) {
            defaultZone = ZoneId.of("UTC");
        }
    }

    /**
     * Returns the default application timezone.
     *
     * @return ZoneId (never null)
     */
	public ZoneId getDefaultZone() {
        return defaultZone;
    }
}
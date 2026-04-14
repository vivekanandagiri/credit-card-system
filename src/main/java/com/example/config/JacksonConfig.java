package com.example.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

/**
 * Jackson configuration for custom serialization.
 *
 * <p>This configuration registers a custom serializer for {@link Instant}
 * to convert timestamps into the user's timezone dynamically.
 *
 * <p><b>How it works:</b>
 * <ul>
 *     <li>Uses {@link InstantSerializer}</li>
 *     <li>Resolves timezone via {@code TimezoneResolver}</li>
 *     <li>Applies conversion before sending API response</li>
 * </ul>
 *
 * <p><b>Flow:</b>
 * <pre>
 * DB (UTC Instant)
 *        ↓
 * Jackson Serializer
 *        ↓
 * TimezoneResolver → TimezoneContext
 *        ↓
 * Convert to user timezone
 *        ↓
 * API Response (localized time)
 * </pre>
 *
 * <p><b>Benefit:</b>
 * No need to manually convert timestamps in services/controllers.
 */
@Configuration
public class JacksonConfig {

    /**
     * Registers custom Jackson module for Instant serialization.
     *
     * @param timezoneResolver resolves user timezone from context
     * @return Jackson module with custom Instant serializer
     */
    @Bean
    public Module instantModule(TimezoneResolver timezoneResolver) {

        SimpleModule module = new SimpleModule();

        // Custom serializer for Instant → user timezone
        module.addSerializer(Instant.class,
                new InstantSerializer(timezoneResolver));

        return module;
    }
}
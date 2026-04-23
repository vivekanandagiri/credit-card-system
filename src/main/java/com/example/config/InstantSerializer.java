package com.example.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
/**
 * Custom Jackson serializer for {@link Instant} that converts timestamps
 * into a timezone-aware ISO-8601 string representation.
 *
 * <p>This serializer resolves the target timezone dynamically using
 * {@link TimezoneResolver}, allowing responses to be formatted based on
 * user, request, or system-specific timezone context.</p>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *     <li>Converts {@link Instant} to {@link ZonedDateTime} using {@link TimezoneResolver}</li>
 *     <li>Formats output using {@link DateTimeFormatter#ISO_OFFSET_DATE_TIME}</li>
 *     <li>Returns {@code null} if input value is null</li>
 * </ul>
 *
 * <p><b>Example Output:</b></p>
 * <pre>
 * 2026-04-22T15:30:00+05:30
 * </pre>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *     <li>API responses requiring localized timestamps</li>
 *     <li>User-specific timezone formatting</li>
 *     <li>Consistent date-time serialization across services</li>
 * </ul>
 *
 * <p><b>Note:</b>
 * The timezone is resolved at runtime. Ensure {@link TimezoneResolver}
 * is correctly implemented to avoid inconsistent output.</p>
 */
public class InstantSerializer extends JsonSerializer<Instant> {
    private final TimezoneResolver timezoneResolver;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public InstantSerializer(TimezoneResolver timezoneResolver) {
        this.timezoneResolver = timezoneResolver;
    }

    @Override
    public void serialize(Instant value,
                          JsonGenerator gen,
                          SerializerProvider serializers) throws IOException {

        if (value == null) {
            gen.writeNull();
            return;
        }

        ZoneId zoneId = timezoneResolver.resolve(null);
        ZonedDateTime zonedDateTime = value.atZone(zoneId);

        gen.writeString(zonedDateTime.format(FORMATTER));
    }
    
}
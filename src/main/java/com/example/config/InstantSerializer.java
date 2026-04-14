package com.example.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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